package com.showmethemoney;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShowMeTheMoneyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShowMeTheMoneyApplication.class, args);
	}

}
