package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir);
		SpringApplication.run(DemoApplication.class, args);
	}

}
