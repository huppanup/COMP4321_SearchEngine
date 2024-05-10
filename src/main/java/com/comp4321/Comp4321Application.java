package com.comp4321;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;

@SpringBootApplication
@RestController
public class Comp4321Application {

	public static void main(String[] args) {
		SpringApplication.run(Comp4321Application.class, args);
	}

	@RequestMapping("/")
	@ResponseBody
	public Resource index(){
		return new ClassPathResource("static/index.html");
	}

	@PostMapping("/search")
	@ResponseBody
	public ResponseEntity<?> handleQuery(@RequestBody String query){
		try {
			Crawler crawler = new Crawler(
					"https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm",
					300,
					"stopwords.txt");
			crawler.crawl();

			Scoring score = new Scoring();
			LinkedHashMap<Integer, Double> result = score.score(query.substring(1, query.length() - 1));
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			e.printStackTrace();
			HashMap<String, String> result = new HashMap<>();
			result.put("success","false");
			return ResponseEntity.ok(result);
		}
	}

}
