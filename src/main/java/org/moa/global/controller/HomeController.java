package org.moa.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.log4j.Log4j2;

@Controller
@Log4j2
public class HomeController {

	@GetMapping("/")
	public String home() {
		log.info("================> HomController /");
		return "index";        // View의 이름
	}
}
