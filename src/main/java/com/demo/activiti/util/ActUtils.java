
package com.demo.activiti.util;

import java.util.List;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.MultiInstanceLoopCharacteristics;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.UserTask;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;

import com.demo.activiti.command.DeleteTaskCmd;
import com.demo.activiti.command.Jump2TargetFlowNodeCommand;
import com.demo.activiti.command.SetFLowNodeAndGoCmd;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ActUtils {

	public static RepositoryService repositoryService = (RepositoryService) SpringContextUtils.getBean("repositoryService", true);
	public static HistoryService historyService = (HistoryService) SpringContextUtils.getBean("historyService", true);
	public static RuntimeService runtimeService = (RuntimeService) SpringContextUtils.getBean("runtimeService", true);
	public static TaskService taskService = (TaskService) SpringContextUtils.getBean("taskService", true);
	public static ObjectMapper objectMapper = (ObjectMapper) SpringContextUtils.getBean("objectMapper", true);
	public static ProcessEngineConfiguration processEngineConfiguration = (ProcessEngineConfiguration) SpringContextUtils.getBean("processEngineConfiguration",
			true);
	public static IdentityService identityService = (IdentityService) SpringContextUtils.getBean("identityService", true);
	public static ManagementService managementService = (ManagementService) SpringContextUtils.getBean("managementService", true);

	/**
	 * 设置会签节点属性 会签相关变量注释：nrOfInstances：实例总数 nrOfActiveInstances：当前活动的，比如，还没完成的，实例数量。 对于顺序执行的多实例，值一直为1 nrOfCompletedInstances：已经完成实例的数目
	 * 可以通过execution.getVariable(x)方法获得这些变量
	 * 
	 * @param modelId
	 *            模型id
	 * @param nodelId
	 *            流程对象id
	 */
	public static void setMultiInstance(String modelId, String nodelId) throws Exception {
		// 获取模型
		byte[] mes = repositoryService.getModelEditorSource(modelId);
		// 转换成JsonNode
		JsonNode jsonNode = objectMapper.readTree(mes);
		// 转换成BpmnModel
		BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();
		BpmnModel bpmnModel = bpmnJsonConverter.convertToBpmnModel(jsonNode);
		// 获取物理形态的流程
		Process process = bpmnModel.getProcesses().get(0);
		// 获取节点信息
		FlowElement flowElement = process.getFlowElement(nodelId);
		// 只有人工任务才可以设置会签节点
		UserTask userTask = (UserTask) flowElement;
		// 设置受理人，这里应该和ElementVariable的值是相同的
		userTask.setAssignee("${" + Constant.ACT_MUIT_VAR_NAME + "}");
		// userTask.setOwner("${user}");

		// 获取多实例配置
		MultiInstanceLoopCharacteristics characteristics = new MultiInstanceLoopCharacteristics();
		// 设置集合变量，统一设置成users
		characteristics.setInputDataItem(Constant.ACT_MUIT_LIST_NAME);
		// 设置变量
		characteristics.setElementVariable(Constant.ACT_MUIT_VAR_NAME);
		// 设置为同时接收（false 表示不按顺序执行）
		characteristics.setSequential(false);
		// 设置条件（暂时处理成，全部会签完转下步）
		characteristics.setCompletionCondition("${nrOfCompletedInstances==nrOfInstances}");

		userTask.setLoopCharacteristics(characteristics);
		// 保存
		ObjectNode objectNode = new BpmnJsonConverter().convertToJson(bpmnModel);
		repositoryService.addModelEditorSource(modelId, objectNode.toString().getBytes("utf-8"));
	}

	/**
	 * 清空会签属性
	 * 
	 * @param modelId
	 *            模型id
	 * @param nodelId
	 *            流程对象id
	 * @throws Exception
	 */
	public static void clearMultiInstance(String modelId, String nodelId) throws Exception {
		// 获取模型
		byte[] mes = repositoryService.getModelEditorSource(modelId);
		// 转换成JsonNode
		JsonNode jsonNode = new ObjectMapper().readTree(mes);
		// 转换成BpmnModel
		BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();
		BpmnModel bpmnModel = bpmnJsonConverter.convertToBpmnModel(jsonNode);
		// 获取物理形态的流程
		Process process = bpmnModel.getProcesses().get(0);
		// 获取节点信息
		FlowElement flowElement = process.getFlowElement(nodelId);
		// 只有人工任务才可以设置会签节点
		UserTask userTask = (UserTask) flowElement;
		// 清空受理人
		userTask.setAssignee("");
		// 获取多实例配置
		MultiInstanceLoopCharacteristics characteristics = userTask.getLoopCharacteristics();
		if (characteristics != null) {
			// 清空集合
			characteristics.setInputDataItem("");
			// 清空变量
			characteristics.setElementVariable("");
			// 设置为顺序接收（true 表示不按顺序执行）
			characteristics.setSequential(true);
			// 清空条件
			characteristics.setCompletionCondition("");
		}

		// 保存
		ObjectNode objectNode = new BpmnJsonConverter().convertToJson(bpmnModel);
		repositoryService.addModelEditorSource(modelId, objectNode.toString().getBytes("utf-8"));
	}

	/**
	 * 增加流程连线条件
	 * 
	 * @param modelId
	 *            模型id
	 * @param nodelId
	 *            流程对象id
	 * @param condition
	 *            el 条件表达式
	 */
	public static void setSequenceFlowCondition(String modelId, String nodelId, String condition) throws Exception {
		// 获取模型--设置连线条件 到 流程中
		byte[] bytes = repositoryService.getModelEditorSource(modelId);
		JsonNode jsonNode = objectMapper.readTree(bytes);
		BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(jsonNode);
		FlowElement flowElement = bpmnModel.getFlowElement(nodelId);
		if (!(flowElement instanceof SequenceFlow)) {
			throw new Exception("不是连线，不能设置条件");
		}
		SequenceFlow sequenceFlow = (SequenceFlow) flowElement;
		sequenceFlow.setConditionExpression(condition);
		ObjectNode objectNode = new BpmnJsonConverter().convertToJson(bpmnModel);
		repositoryService.addModelEditorSource(modelId, objectNode.toString().getBytes("utf-8"));
	}

	/************************* 回退开始 ***************************/

	/**
	 * 根据任务ID获取对应的流程实例
	 *
	 * @param taskId
	 *            任务ID
	 * @return
	 * @throws Exception
	 */
	public static ProcessInstance findProcessInstanceByTaskId(String taskId) throws Exception {
		// 找到流程实例
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(findTaskById(taskId).getProcessInstanceId())
				.singleResult();
		if (processInstance == null) {
			throw new Exception("流程实例未找到!");
		}
		return processInstance;
	}

	/**
	 * 根据任务ID获得任务实例
	 * 
	 * @param taskId
	 *            任务ID
	 * @return TaskEntity
	 * @throws Exception
	 */
	private static TaskEntity findTaskById(String taskId) throws Exception {
		TaskEntity task = (TaskEntity) taskService.createTaskQuery().taskId(taskId).singleResult();
		if (task == null) {
			throw new Exception("任务实例未找到!");
		}
		return task;
	}

	/**
	 * 设置任务处理人,根据任务ID
	 * 
	 * @param taskId
	 * @param userCode
	 */
	public static void setTaskDealerByTaskId(String taskId, String userCode) {
		taskService.setAssignee(taskId, userCode);
	}

	/**
	 * 根据流程对象Id,查询当前节点Id
	 * 
	 * @param executionId
	 * @return
	 */
	public static String getActiviIdByExecutionId(String executionId) {
		// 根据任务获取当前流程执行ID，执行实例以及当前流程节点的ID：
		ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(executionId).singleResult();
		String activitiId = execution.getActivityId();
		return activitiId;
	}

	/**
	 * 根据流程实例ID和任务key值查询所有同级任务集合
	 *
	 * @param processInstanceId
	 * @param key
	 * @return
	 */
	public static List<Task> findTaskListByKey(String processInstanceId, String key) {
		List<Task> list = taskService.createTaskQuery().processInstanceId(processInstanceId).taskDefinitionKey(key).list();
		return list;
	}

	/**
	 * 根据任务ID获取流程定义
	 *
	 * @param taskId
	 *            任务ID
	 * @return
	 * @throws Exception
	 */
	public static ProcessDefinitionEntity findProcessDefinitionEntityByTaskId(String taskId) throws Exception {
		// 取得流程定义
		ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
				.getDeployedProcessDefinition(findTaskById(taskId).getProcessDefinitionId());

		if (processDefinition == null) {
			throw new Exception("流程定义未找到!");
		}

		return processDefinition;
	}

	/**
	 * 根据任务Id,查找当前任务
	 * 
	 * @param taskId
	 *            任务Id
	 * @return
	 */
	public static Task getTaskById(String taskId) {
		// 当前处理人的任务
		Task currenTask = taskService.createTaskQuery().taskId(taskId).singleResult();
		return currenTask;
	}

	/**
	 * 根据流程实例查询正在执行的任务
	 * 
	 * @param processInst
	 * @return
	 */
	public static List<Task> getTaskByProcessInst(String processInst) {
		List<Task> list = taskService.createTaskQuery().processInstanceId(processInst).list();
		return list;
	}

	/*
	 * /** 顺序会签后退时处理
	 * 
	 * @param instanceId 流程实例
	 * 
	 * @param comment 意见
	 * 
	 * @param preActivityId 上个已经完成任务ID
	 */
	public static void dealMultiSequential(String instanceId, String comment, String preActivityId) {
		// 该流程实例正在运行的任务
		List<Task> runTask = getTaskByProcessInst(instanceId);
		for (Task t : runTask) {
			String runActivityId = t.getTaskDefinitionKey();
			// 正在运行的任务节点id和上个已经完成的任务节点id相等,则判定为顺序会签
			if (runActivityId.equals(preActivityId)) {
				if (comment == null) {
					comment = "";
				}
				taskService.addComment(t.getId(), t.getProcessInstanceId(), comment);
				// 执行转向任务
				t.setDescription("callback");
				taskService.saveTask(t);
				taskService.complete(t.getId());
				// 递归顺序会签,直到正在运行的任务还是顺序会签
				dealMultiSequential(t.getProcessInstanceId(), comment, t.getTaskDefinitionKey());
			}
		}
	}

	/**
	 * 设置回退的任务处理人
	 * 
	 * @param task
	 *            当前任务
	 * @param activityId
	 *            回退节点ID
	 */
	public static void setBackTaskDealer(Task task, String activityId) {
		List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().processInstanceId(task.getProcessInstanceId())
				.taskDefinitionKey(activityId).taskDeleteReason("completed").orderByTaskCreateTime().desc().list();
		HistoricTaskInstance historicTask = null;
		if (list != null && list.size() > 0) {
			historicTask = list.get(0);
			// 查询回退后的节点正在运行的任务
			List<Task> taskList = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).taskDefinitionKey(activityId).active().list();
			// 同一节点下有多个任务，则认定为会签任务
			if (taskList != null && taskList.size() > 1) {
				for (int i = 0; i < taskList.size(); i++) {
					// 设置会签任务处理人（处理人顺序不管）
					taskService.setAssignee(taskList.get(i).getId(), list.get(i).getAssignee());
				}
			} else {
				Task taskNew = taskList.get(0);
				// 顺序会签流程变量处理人
				String variable = (String) runtimeService.getVariable(taskNew.getExecutionId(), "countersign");
				if (!StringUtils.isEmpty(variable)) {
					// 设置下个顺序会签处理人
					setTaskDealerByTaskId(taskNew.getId(), variable);
				} else {
					// 设置一般回退任务处理人
					taskService.setAssignee(taskNew.getId(), historicTask.getAssignee());
				}
			}
		}
	}

	/**
	 * 转办流程
	 * 
	 * @param taskId
	 *            当前任务节点ID
	 * @param userId
	 *            被转办人id
	 */
	public static boolean transferAssignee(String taskId, String userId) {
		try {
			taskService.setAssignee(taskId, userId);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 跳转到指定流程节点
	 * 
	 * @param curTaskId
	 * @param targetFlowNodeId
	 *            指定的流程节点ID 比如跳转<endEvent id="endevent1" name="End"></endEvent> ，则targetFlowNodeId为endevent1
	 */
	public static void jump2TargetFlowNode(String curTaskId, String targetFlowNodeId) {
		managementService.executeCommand(new Jump2TargetFlowNodeCommand(curTaskId, targetFlowNodeId));
	}

	// 跳转方法
	public static void jump(String taskId, String targetFlowNodeId) {
		// 当前任务
		Task currentTask = taskService.createTaskQuery().taskId(taskId).singleResult();
		// 获取流程定义
		Process process = repositoryService.getBpmnModel(currentTask.getProcessDefinitionId()).getMainProcess();
		// 获取目标节点定义
		FlowNode targetNode = (FlowNode) process.getFlowElement(targetFlowNodeId);
		// 删除当前运行任务
		String executionEntityId = managementService.executeCommand(new DeleteTaskCmd(currentTask.getId()));
		// 流程执行到来源节点
		managementService.executeCommand(new SetFLowNodeAndGoCmd(targetNode, executionEntityId));
	}
}
