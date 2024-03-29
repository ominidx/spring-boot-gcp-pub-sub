package com.example.demo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.google.cloud.pubsub.v1.Subscriber;

@RestController
public class WebController {

	private static final Log LOGGER = LogFactory.getLog(DemoApplication.class);

	private final PubSubTemplate pubSubTemplate;

	private final PubSubAdmin pubSubAdmin;

	private final ArrayList<Subscriber> allSubscribers;

	public WebController(PubSubTemplate pubSubTemplate, PubSubAdmin pubSubAdmin) {
		this.pubSubTemplate = pubSubTemplate;
		this.pubSubAdmin = pubSubAdmin;
		this.allSubscribers = new ArrayList<>();
	}

	@PostMapping(value = "/createTopic")
	public String createTopic(@RequestParam("topicName") String topicName) {
		this.pubSubAdmin.createTopic(topicName);

		return "Topic creation successful.";
	}

	@PostMapping("/createSubscription")
	public String createSubscription(@RequestParam("topicName") String topicName,
			@RequestParam("subscriptionName") String subscriptionName) {
		this.pubSubAdmin.createSubscription(subscriptionName, topicName);

		return"Subscription creation successful.";
	}

	@GetMapping("/postMessage")
	public String publish(@RequestParam("topicName") String topicName, @RequestParam("message") String message,
			@RequestParam("count") int messageCount) {
		for (int i = 0; i < messageCount; i++) {
			this.pubSubTemplate.publish(topicName, message);
		}

		return "Messages published asynchronously; status unknown.";
	}

	@GetMapping("/pull")
	public Collection<AcknowledgeablePubsubMessage> pull(@RequestParam("subscription") String subscriptionName) {

		Collection<AcknowledgeablePubsubMessage> messages = this.pubSubTemplate.pull(subscriptionName, 10, true);

		if (messages.isEmpty()) {
			return messages;
		}

		String returnView;
		try {
			ListenableFuture<Void> ackFuture = this.pubSubTemplate.ack(messages);
			ackFuture.get();
			returnView = String.format("Pulled and acked %s message(s)", messages.size());
			System.out.println(messages.toString());
			LOGGER.info(messages.toString());
		} catch (Exception ex) {
			LOGGER.warn("Acking failed.", ex);
			returnView = "Acking failed";
		}

		return messages;
	}

	@GetMapping("/multipull")
	public RedirectView multipull(@RequestParam("subscription1") String subscriptionName1,
			@RequestParam("subscription2") String subscriptionName2) {

		Set<AcknowledgeablePubsubMessage> mixedSubscriptionMessages = new HashSet<>();
		mixedSubscriptionMessages.addAll(this.pubSubTemplate.pull(subscriptionName1, 1000, true));
		mixedSubscriptionMessages.addAll(this.pubSubTemplate.pull(subscriptionName2, 1000, true));

		if (mixedSubscriptionMessages.isEmpty()) {
			return buildStatusView("No messages available for retrieval.");
		}

		RedirectView returnView;
		try {
			ListenableFuture<Void> ackFuture = this.pubSubTemplate.ack(mixedSubscriptionMessages);
			ackFuture.get();
			returnView = buildStatusView(
					String.format("Pulled and acked %s message(s)", mixedSubscriptionMessages.size()));
		} catch (Exception ex) {
			LOGGER.warn("Acking failed.", ex);
			returnView = buildStatusView("Acking failed");
		}

		return returnView;
	}

	@GetMapping("/subscribe")
	public String subscribe(@RequestParam("subscription") String subscriptionName) {
		Subscriber subscriber = this.pubSubTemplate.subscribe(subscriptionName, (message) -> {
			LOGGER.info("Message received from " + subscriptionName + " subscription: "
					+ message.getPubsubMessage().getData().toStringUtf8());
			message.ack();
		});

		this.allSubscribers.add(subscriber);
		return "Subscribed.";
	}

	@PostMapping("/deleteTopic")
	public RedirectView deleteTopic(@RequestParam("topic") String topicName) {
		this.pubSubAdmin.deleteTopic(topicName);

		return buildStatusView("Topic deleted successfully.");
	}

	@PostMapping("/deleteSubscription")
	public RedirectView deleteSubscription(@RequestParam("subscription") String subscriptionName) {
		this.pubSubAdmin.deleteSubscription(subscriptionName);

		return buildStatusView("Subscription deleted successfully.");
	}

	private RedirectView buildStatusView(String statusMessage) {
		RedirectView view = new RedirectView("/");
		view.addStaticAttribute("statusMessage", statusMessage);
		return view;
	}
}
