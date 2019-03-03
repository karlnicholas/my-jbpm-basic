package com.aexp.isap.ots.bpm.mcservice.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mcd")
public class McResources {
	
	@RequestMapping("/test")
	public String test() {
		return "test";
	}
}
