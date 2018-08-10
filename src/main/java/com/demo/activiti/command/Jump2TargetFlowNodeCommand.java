package com.demo.activiti.command;

import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.engine.ActivitiEngineAgenda;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntityManager;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntityManager;
import org.activiti.engine.impl.util.ProcessDefinitionUtil;

/**
 * 跳转到指定节点代码
 * 
 * @author linyb-geek
 *
 */
public class Jump2TargetFlowNodeCommand implements Command<Void> {
	private String curTaskId;

	private String targetFlowNodeId;

	public Jump2TargetFlowNodeCommand(String curTaskId, String targetFlowNodeId) {
		super();
		this.curTaskId = curTaskId;
		this.targetFlowNodeId = targetFlowNodeId;
	}

	@Override
	public Void execute(CommandContext commandContext) {
		System.out.println("跳转到目标流程节点：" + targetFlowNodeId);
		ExecutionEntityManager executionEntityManager = commandContext.getExecutionEntityManager();
		TaskEntityManager taskEntityManager = commandContext.getTaskEntityManager();
		// 获取当前任务的来源任务及来源节点信息
		TaskEntity taskEntity = taskEntityManager.findById(curTaskId);
		ExecutionEntity executionEntity = executionEntityManager.findById(taskEntity.getExecutionId());
		Process process = ProcessDefinitionUtil.getProcess(executionEntity.getProcessDefinitionId());
		// 删除当前节点
		taskEntityManager.deleteTask(taskEntity, "", true, true);
		// 获取要跳转的目标节点
		FlowElement targetFlowElement = process.getFlowElement(targetFlowNodeId);
		executionEntity.setCurrentFlowElement(targetFlowElement);
		ActivitiEngineAgenda agenda = commandContext.getAgenda();
		agenda.planContinueProcessInCompensation(executionEntity);

		return null;
	}

	public String getCurTaskId() {
		return curTaskId;
	}

	public void setCurTaskId(String curTaskId) {
		this.curTaskId = curTaskId;
	}

	public String getTargetFlowNodeId() {
		return targetFlowNodeId;
	}

	public void setTargetFlowNodeId(String targetFlowNodeId) {
		this.targetFlowNodeId = targetFlowNodeId;
	}

}
