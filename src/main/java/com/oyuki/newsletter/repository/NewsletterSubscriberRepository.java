package com.oyuki.newsletter.repository;

import com.oyuki.newsletter.entity.NewsletterSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, Long> {
    Optional<NewsletterSubscriber> findByEmailIgnoreCase(String email);
    List<NewsletterSubscriber> findAllByActiveTrueOrderBySubscribedAtDesc();
    long countByActiveTrue();
}
