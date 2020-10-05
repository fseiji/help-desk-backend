package com.fseiji.helpdesk.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.fseiji.helpdesk.api.entity.Ticket;

public interface TicketRepository extends MongoRepository<Ticket, String> {

	// Containing equivalente ao LIKE
	// IgnoreCase ignora se é maiúsculo ou minúsculo

	Page<Ticket> findByUserIdOrderByDateDesc(Pageable pages, String userId);

	Page<Ticket> findByTitleIgnoreCaseContainingAndStatusContainingAndPriorityContainingOrderByDateDesc(String title, String status,
			String priority, Pageable pages);

	Page<Ticket> findByTitleIgnoreCaseContainingAndStatusContainingAndPriorityContainingAndUserIdOrderByDateDesc(String title,
			String status, String priority, String userId, Pageable pages);

	Page<Ticket> findByTitleIgnoreCaseContainingAndStatusContainingAndPriorityContainingAndAssignedUserIdOrderByDateDesc(String title,
			String status, String priority, String userId, Pageable pages);

	Page<Ticket> findByNumber(Integer number, Pageable pages);
}
