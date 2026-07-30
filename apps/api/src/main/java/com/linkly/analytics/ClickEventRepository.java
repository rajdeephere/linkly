package com.linkly.analytics;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickEventRepository extends JpaRepository<ClickEvent, UUID> {

    long countByLinkCode(String linkCode);
}
