package com.fseiji.helpdesk.api.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fseiji.helpdesk.api.dto.Summary;
import com.fseiji.helpdesk.api.entity.ChangeStatus;
import com.fseiji.helpdesk.api.entity.Ticket;
import com.fseiji.helpdesk.api.entity.User;
import com.fseiji.helpdesk.api.enums.ProfileEnum;
import com.fseiji.helpdesk.api.enums.StatusEnum;
import com.fseiji.helpdesk.api.response.Response;
import com.fseiji.helpdesk.api.security.jwt.JwtTokenUtil;
import com.fseiji.helpdesk.api.service.TicketService;
import com.fseiji.helpdesk.api.service.UserService;

@RestController
@RequestMapping("/api/ticket")
@CrossOrigin(origins = "*")
public class TicketController {

	@Autowired
	private TicketService ticketService;
	@Autowired
	protected JwtTokenUtil jwtTokenUtil;
	@Autowired
	private UserService userService;

	@PostMapping()
	@PreAuthorize("hasAnyRole('CUSTOMER')")
	public ResponseEntity<Response<Ticket>> createOrUpdate(HttpServletRequest request, @RequestBody Ticket ticket,
			BindingResult result) {
		Response<Ticket> response = new Response<Ticket>();
		try {
			validateCreateTicket(ticket, result);
			if (result.hasErrors()) {
				result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
				return ResponseEntity.badRequest().body(response);
			}
			ticket.setStatus(StatusEnum.getStatus("New"));
			ticket.setUser(userFromRequest(request));
			ticket.setDate(new Date());
			ticket.setNumber(generateNumber());
			Ticket ticketPersisted = this.ticketService.createOrUpdate(ticket);
			response.setData(ticketPersisted);
		} catch (Exception e) {
			response.getErrors().add(e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
		return ResponseEntity.ok().body(response);
	}

	private void validateCreateTicket(Ticket ticket, BindingResult result) {
		if (ticket.getTitle() == null) {
			result.addError(new ObjectError("Ticket", "Title no information"));
			return;
		}
	}

	private User userFromRequest(HttpServletRequest request) {
		String token = request.getHeader("Authorization");
		String email = this.jwtTokenUtil.getUsernameFromToken(token);
		return this.userService.findByEmail(email);
	}

	private Integer generateNumber() {
		Random random = new Random();
		return random.nextInt(9999);
	}

	@PutMapping()
	@PreAuthorize("hasAnyRole('CUSTOMER')")
	public ResponseEntity<Response<Ticket>> update(HttpServletRequest request, @RequestBody Ticket ticket,
			BindingResult result) {
		Response<Ticket> response = new Response<Ticket>();
		try {
			validateUpdateTicket(ticket, result);
			if (result.hasErrors()) {
				result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
				return ResponseEntity.badRequest().body(response);
			}
			Ticket ticketCurrent = this.ticketService.findById(ticket.getId()).get();
			ticket.setStatus(ticketCurrent.getStatus());
			ticket.setUser(ticketCurrent.getUser());
			ticket.setDate(ticketCurrent.getDate());
			ticket.setNumber(ticketCurrent.getNumber());
			if (ticketCurrent.getAssignedUser() != null) {
				ticket.setAssignedUser(ticketCurrent.getAssignedUser());
			}
			Ticket ticketPersisted = this.ticketService.createOrUpdate(ticket);
			response.setData(ticketPersisted);
		} catch (Exception e) {
			response.getErrors().add(e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
		return ResponseEntity.ok().body(response);
	}

	private void validateUpdateTicket(Ticket ticket, BindingResult result) {
		if (ticket.getId() == null) {
			result.getAllErrors().add(new ObjectError("Ticket", "Id no information"));
		}

		if (ticket.getTitle() == null) {
			result.getAllErrors().add(new ObjectError("Ticket", "Title no information"));
		}

	}

	@GetMapping(value = "/{id}")
	@PreAuthorize("hasAnyRole('CUSTOMER','TECHNICAN')")
	public ResponseEntity<Response<Ticket>> findById(@PathVariable("id") String id) {
		Response<Ticket> response = new Response<Ticket>();
		Ticket ticket = this.ticketService.findById(id).get();
		if (ticket == null) {
			response.getErrors().add("Register not found id: " + id);
			return ResponseEntity.badRequest().body(response);
		}
		List<ChangeStatus> changes = new ArrayList<ChangeStatus>();
		Iterable<ChangeStatus> changesCurrent = this.ticketService.listChangeStatus(ticket.getId());
		for (Iterator<ChangeStatus> iterator = changesCurrent.iterator(); iterator.hasNext();) {
			ChangeStatus changeStatus = iterator.next();
			changeStatus.setTicket(null);
			changes.add(changeStatus);
		}
		ticket.setChanges(changes);
		response.setData(ticket);
		return ResponseEntity.ok().body(response);
	}

	@DeleteMapping(value = "/{id}")
	@PreAuthorize("hasAnyRole('CUSTOMER')")
	public ResponseEntity<Response<String>> delete(@PathVariable("id") String id) {
		Response<String> response = new Response<String>();
		Ticket ticket = this.ticketService.findById(id).get();
		if (ticket == null) {
			response.getErrors().add("Register not found id: " + id);
			return ResponseEntity.badRequest().body(response);
		}
		this.ticketService.delete(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping(value = "{page}/{count}")
	@PreAuthorize("hasAnyRole('CUSTOMER','TECHNICAN')")
	public ResponseEntity<Response<Page<Ticket>>> findAll( 
			@PathVariable("page") int page,
			@PathVariable("count") int count,
			HttpServletRequest request) {
		Response<Page<Ticket>> response = new Response<Page<Ticket>>();
		Page<Ticket> tickets = null;
		User userRequest = userFromRequest(request);
		if (userRequest.getProfile().equals(ProfileEnum.ROLE_TECHNICAN)) {
			tickets = this.ticketService.listTicket(page, count);
		} else if (userRequest.getProfile().equals(ProfileEnum.ROLE_CUSTOMER)) {
			tickets = this.ticketService.findByCurrentUser(page, count, userRequest.getId());
		}
		response.setData(tickets);
		return ResponseEntity.ok(response);
	}

	@GetMapping(value = "/{page}/{count}/{number}/{title}/{status}/{priority}/{assigned}")
	@PreAuthorize("hasAnyRole('CUSTOMER','TECHNICAN')")
	public ResponseEntity<Response<Page<Ticket>>> findByParam(HttpServletRequest request,
			@PathVariable("page") int page, @PathVariable("count") int count, @PathVariable("number") Integer number,
			@PathVariable("title") String title, @PathVariable("status") String status,
			@PathVariable("priority") String priority, 
			@PathVariable("assigned") boolean assigned) {

		title = title.equals("uninformed") ? "" : title;
		status = status.equals("uninformed") ? "" : status;
		priority = priority.equals("uninformed") ? "" : priority;
		Response<Page<Ticket>> response = new Response<Page<Ticket>>();
		Page<Ticket> tickets = null;
		if (number > 0) {
			tickets = this.ticketService.findByNumber(page, count, number);
		} else {
			User userRequest = userFromRequest(request);
			if (userRequest.getProfile().equals(ProfileEnum.ROLE_TECHNICAN)) {
				if (assigned) {
					tickets = this.ticketService.findByParametersAndAssignedUser(page, count, title, status, priority,
							userRequest.getId());
				} else {
					tickets = this.ticketService.findByParameters(page, count, title, status, priority);
				}
			} else if (userRequest.getProfile().equals(ProfileEnum.ROLE_CUSTOMER)) {
				tickets = this.ticketService.findByParametersAndCurrentUser(page, count, title, status, priority,
						userRequest.getId());
			}
		}
		response.setData(tickets);
		return ResponseEntity.ok(response);
	}

	@PutMapping(value = "/{id}/{status}")
	@PreAuthorize("hasAnyRole('CUSTOMER','TECHNICAN')")
	public ResponseEntity<Response<Ticket>> changeStatus(HttpServletRequest request, @RequestBody Ticket ticket,
			BindingResult result, @PathVariable("id") String id, @PathVariable("status") String status) {
		Response<Ticket> response = new Response<Ticket>();
		try {
			validateChangeStatus(id, status, result);
			if (result.hasErrors()) {
				result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
				return ResponseEntity.badRequest().body(response);
			}
			Ticket ticketCurrent = this.ticketService.findById(id).get();
			ticketCurrent.setStatus(StatusEnum.getStatus(status));
			if (status.equals("Assigned")) {
				ticketCurrent.setAssignedUser(userFromRequest(request));
			}
			Ticket ticketPersisted = this.ticketService.createOrUpdate(ticketCurrent);
			ChangeStatus changeStatus = new ChangeStatus();
			changeStatus.setUserChange(userFromRequest(request));
			changeStatus.setDateChangeStatus(new Date());
			changeStatus.setStatus(StatusEnum.getStatus(status));
			changeStatus.setTicket(ticketPersisted);
			this.ticketService.createChangeStatus(changeStatus);
			response.setData(ticketPersisted);
		} catch (Exception e) {
			response.getErrors().add(e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
		return ResponseEntity.ok(response);
	}

	private void validateChangeStatus(String id, String status, BindingResult result) {
		if (id == null || id.isEmpty()) {
			result.getAllErrors().add(new ObjectError("Ticket", "Id no information"));
		}

		if (status == null || status.isEmpty()) {
			result.getAllErrors().add(new ObjectError("Ticket", "Status no information"));
		}
	}

	@GetMapping(value = "/summary")
	@PreAuthorize("hasAnyRole('CUSTOMER','TECHNICAN')")
	public ResponseEntity<Response<Summary>> findSummary() {
		Response<Summary> response = new Response<Summary>();
		Summary summary = new Summary();
		int amountNew = 0;
		int amountResolved = 0;
		int amountApproved = 0;
		int amountDisapproved = 0;
		int amountAssigned = 0;
		int amountClosed = 0;

		Iterable<Ticket> tickets = this.ticketService.findAll();
		if (tickets != null) {
			for (Iterator<Ticket> iterator = tickets.iterator(); iterator.hasNext();) {
				Ticket ticket = (Ticket) iterator.next();
				if (ticket.getStatus().equals(StatusEnum.NEW)) {
					amountNew++;
				} else if (ticket.getStatus().equals(StatusEnum.RESOLVED)) {
					amountResolved++;
				} else if (ticket.getStatus().equals(StatusEnum.APPROVED)) {
					amountApproved++;
				} else if (ticket.getStatus().equals(StatusEnum.DISAPPROVED)) {
					amountDisapproved++;
				} else if (ticket.getStatus().equals(StatusEnum.ASSIGNED)) {
					amountAssigned++;
				} else if (ticket.getStatus().equals(StatusEnum.CLOSED)) {
					amountClosed++;
				}
			}
			summary.setAmountNew(amountNew);
			summary.setAmountResolved(amountResolved);
			summary.setAmountApproved(amountApproved);
			summary.setAmountDisapproved(amountDisapproved);
			summary.setAmountAssigned(amountAssigned);
			summary.setAmountClosed(amountClosed);
		}
		response.setData(summary);
		return ResponseEntity.ok(response);
	}
}
