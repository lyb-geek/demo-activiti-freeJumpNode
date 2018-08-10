package com.demo.activiti;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CandidateUsersService implements Serializable {

	public List<String> getCandidateUsers() {
		List<String> candidateUsers = new ArrayList<>();
		candidateUsers.add("张三");
		candidateUsers.add("李四");
		return candidateUsers;
	}

}
