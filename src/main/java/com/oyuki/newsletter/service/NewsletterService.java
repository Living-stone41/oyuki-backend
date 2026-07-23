package com.oyuki.newsletter.service;

import com.oyuki.newsletter.entity.NewsletterSubscriber;
import com.oyuki.newsletter.repository.NewsletterSubscriberRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class NewsletterService {
    private final NewsletterSubscriberRepository repository;
    private final JavaMailSender mailSender;
    private final String senderEmail;

    public NewsletterService(NewsletterSubscriberRepository repository,
                             ObjectProvider<JavaMailSender> mailProvider,
                             @Value("${app.mail.from:}") String senderEmail) {
        this.repository = repository;
        this.mailSender = mailProvider.getIfAvailable();
        this.senderEmail = senderEmail == null ? "" : senderEmail.trim();
    }

    @Transactional
    public Map<String, Object> subscribe(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        NewsletterSubscriber subscriber = repository.findByEmailIgnoreCase(email)
                .orElseGet(() -> NewsletterSubscriber.builder().email(email).build());
        subscriber.setActive(true);
        repository.save(subscriber);
        sendWelcome(email);
        return Map.of("email", email, "subscribed", true);
    }

    @Transactional
    public Map<String, Object> unsubscribe(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        repository.findByEmailIgnoreCase(email).ifPresent(s -> {
            s.setActive(false);
            repository.save(s);
        });
        return Map.of("email", email, "subscribed", false);
    }

    public Map<String, Object> send(String subject, String body) {
        requireMail();
        List<NewsletterSubscriber> subscribers = repository.findAllByActiveTrueOrderBySubscribedAtDesc();
        int sent = 0;
        List<String> failed = new ArrayList<>();
        for (NewsletterSubscriber subscriber : subscribers) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(senderEmail);
                message.setTo(subscriber.getEmail());
                message.setSubject(subject.trim());
                message.setText(body.trim() + "\n\n— Oyuki Marketplace\nTo unsubscribe, visit the Oyuki website footer.");
                mailSender.send(message);
                sent++;
            } catch (Exception ex) {
                failed.add(subscriber.getEmail());
            }
        }
        return Map.of("subscribers", subscribers.size(), "sent", sent, "failed", failed.size());
    }

    public Map<String, Object> summary() {
        return Map.of("activeSubscribers", repository.countByActiveTrue());
    }

    private void sendWelcome(String email) {
        if (mailSender == null || senderEmail.isBlank()) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(email);
            message.setSubject("Welcome to the Oyuki newsletter");
            message.setText("You are subscribed to Oyuki updates. You will receive marketplace news, offers and Farmers' Day announcements.\n\n— Oyuki Marketplace");
            mailSender.send(message);
        } catch (Exception ignored) { }
    }

    private void requireMail() {
        if (mailSender == null || senderEmail.isBlank()) {
            throw new IllegalStateException("Newsletter email delivery is not configured");
        }
    }
}
