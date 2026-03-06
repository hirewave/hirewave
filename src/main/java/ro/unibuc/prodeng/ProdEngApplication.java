package ro.unibuc.prodeng;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import ro.unibuc.prodeng.repository.ClientRepository;
import ro.unibuc.prodeng.repository.FreelancerRepository;
import ro.unibuc.prodeng.repository.UserRepository;
import ro.unibuc.prodeng.request.*;
import ro.unibuc.prodeng.response.ClientResponse;
import ro.unibuc.prodeng.response.FreelancerResponse;
import ro.unibuc.prodeng.response.ProjectResponse;
import ro.unibuc.prodeng.service.*;

import jakarta.annotation.PostConstruct;

import java.util.List;

@SpringBootApplication
@EnableMongoRepositories
public class ProdEngApplication {

	@Autowired
	private UserService userService;

	@Autowired
	private TodoService todoService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FreelancerService freelancerService;

	@Autowired
	private ClientService clientService;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private BidService bidService;

	@Autowired
	private FreelancerRepository freelancerRepository;

	@Autowired
	private ClientRepository clientRepository;

	public static void main(String[] args) {
		SpringApplication.run(ProdEngApplication.class, args);
	}

	@PostConstruct
	public void runAfterObjectCreated() {
		// Keep original seed data
		if (userRepository.findByEmail("frodo@theshire.me").isEmpty()) {
			CreateUserRequest userRequest = new CreateUserRequest("Frodo Baggins", "frodo@theshire.me");
			userService.createUser(userRequest);
			todoService.createTodo(new CreateTodoRequest("Take the ring to Mordor", "frodo@theshire.me"));
		}

		// Seed HireWave demo data
		if (freelancerRepository.findByEmail("alice@dev.io").isEmpty()) {
			FreelancerResponse f1 = freelancerService.createFreelancer(
					new CreateFreelancerRequest("Alice Developer", "alice@dev.io", List.of("Java", "Spring Boot", "MongoDB"), 75.0));
			FreelancerResponse f2 = freelancerService.createFreelancer(
					new CreateFreelancerRequest("Bob Designer", "bob@design.io", List.of("UI/UX", "Figma", "CSS", "React"), 60.0));
			freelancerService.createFreelancer(
					new CreateFreelancerRequest("Charlie Fullstack", "charlie@code.io", List.of("Java", "React", "Docker", "AWS"), 90.0));

			ClientResponse c1 = clientService.createClient(new CreateClientRequest("TechCorp", "hire@techcorp.com"));
			ClientResponse c2 = clientService.createClient(new CreateClientRequest("StartupXYZ", "jobs@startupxyz.com"));

			ProjectResponse p1 = projectService.createProject(
					new CreateProjectRequest("E-Commerce Backend", "Build a REST API for an e-commerce platform", List.of("Java", "Spring Boot"), 5000.0, c1.id()));
			ProjectResponse p2 = projectService.createProject(
					new CreateProjectRequest("Landing Page Redesign", "Redesign the company landing page", List.of("UI/UX", "CSS", "React"), 3000.0, c2.id()));

			bidService.createBid(new CreateBidRequest(p1.id(), f1.id(), 4500.0));
			bidService.createBid(new CreateBidRequest(p2.id(), f2.id(), 2800.0));
		}
	}
}
