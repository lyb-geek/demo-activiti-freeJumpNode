package com.demo.activiti;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

import com.demo.activiti.util.ActUtils;

public class LeaveMain {
	public static void main(String[] args) {

		RepositoryService repositoryService = ActUtils.repositoryService;

		IdentityService identityService = ActUtils.identityService;

		ManagementService managementService = ActUtils.managementService;

		HistoryService historyService = ActUtils.historyService;

		RuntimeService runtimeService = ActUtils.runtimeService;

		TaskService taskService = ActUtils.taskService;

		Deployment deployment = repositoryService.createDeployment().addClasspathResource("leave.bpmn").deploy();

		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();

		identityService.setAuthenticatedUserId("applyUser");

		Map<String, Object> vars = new HashMap<>();
		vars.put("candidateUsersService", new CandidateUsersService());
		ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId(), vars);

		Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskAssignee("applyUser").singleResult();
		taskService.complete(task.getId());

		task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskCandidateUser("张三").singleResult();
		taskService.setAssignee(task.getId(), "张三");

		Scanner scanner = new Scanner(System.in);
		System.out.println("是否跳转指定流程...");
		String isJumpTargetNode = scanner.nextLine();

		if ("no".equals(isJumpTargetNode)) {
			System.out.println("----流程按流程图顺序流转-------");
			taskService.complete(task.getId());
			task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskAssignee("hr").singleResult();
			taskService.complete(task.getId());
		} else {
			if ("yes".equals(isJumpTargetNode)) {
				System.out.println("请输入要跳转的目标节点ID...");
				String targetFlowNodeId = scanner.nextLine();
				// 方法一
				ActUtils.jump2TargetFlowNode(task.getId(), targetFlowNodeId);
				// 方法二
				// ActUtils.jump(taskId, targetFlowNodeId);
			}
		}

		System.out.println("----------------------------------流程实例流转-----------------------");
		List<HistoricActivityInstance> list = historyService // 历史相关Service
				.createHistoricActivityInstanceQuery() // 创建历史活动实例查询
				.processInstanceId(processInstance.getId()) // 执行流程实例id
				.finished().list();
		for (HistoricActivityInstance hai : list) {
			System.out.println("活动ID:" + hai.getId());
			System.out.println("流程实例ID:" + hai.getProcessInstanceId());
			System.out.println("活动名称：" + hai.getActivityName());
			System.out.println("办理人：" + hai.getAssignee());
			System.out.println("开始时间：" + hai.getStartTime());
			System.out.println("结束时间：" + hai.getEndTime());
			System.out.println("=================================");
		}

	}

}
