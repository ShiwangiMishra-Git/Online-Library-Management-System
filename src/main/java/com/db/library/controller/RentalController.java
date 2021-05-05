package com.db.library.controller;

import java.io.Console;
import java.math.BigInteger;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Tuple;
import javax.sql.DataSource;

import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.db.library.model.Author;
import com.db.library.model.Book;
import com.db.library.model.BookCopy;
import com.db.library.model.Invoice;
import com.db.library.model.Payment;
import com.db.library.model.Rental;
import com.db.library.model.RentalHistory;
import com.db.library.model.Topic;
import com.db.library.repository.AuthorRepository;
import com.db.library.repository.BookCopyRepository;
import com.db.library.repository.BookRepository;
import com.db.library.repository.CustomerRepository;
import com.db.library.repository.InvoiceRepository;
import com.db.library.repository.PaymentRepository;
import com.db.library.repository.RentalRepository;
import com.db.library.repository.TopicRepository;

@Controller
public class RentalController {

	

	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private DataSource datasource;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private BookCopyRepository bookCopyRepository;

	@Autowired
	private AuthorRepository authorRepository;
	
	@Autowired
	private PaymentRepository paymentRepository;
	
	@Autowired
	private InvoiceRepository invoiceRepository;

	@Autowired
	private RentalRepository rentalRepository;
	
	@Autowired
	private CustomerRepository customerRepository;

	@RequestMapping(value = "/a/rentals", method = RequestMethod.GET)
	public String booksListwithStatus(Model model,Principal p) {
		int customerId = customerRepository.findByEmailAddress(p.getName()).getId();
		Session session = sessionFactory.openSession();
		Transaction tx = session.beginTransaction();
		List<Book> bookList = bookRepository.findByDeleted(0);

		for (int i = 0; i < bookList.size(); i++) {

			SimpleJdbcCall jdbcCall = new SimpleJdbcCall(datasource).withFunctionName("checkBookAvailabiltyStatus");
			// call for function
			SqlParameterSource in = new MapSqlParameterSource().addValue("book_id", bookList.get(i).getId());
			System.out.println("calling for" + in.getValue("book_id"));
			int copyid = 0;
			try {

				copyid = jdbcCall.executeFunction(Integer.class, in);

			} catch (Exception ex) {
				System.out.println("Exception while running checkBookAvailabiltyStatus for" + in.getValue("book_id"));
			}
			bookList.get(i).setAvailableCopyId(copyid);
			bookList.get(i).setMakerentable();
		}

		/*
		 * SimpleJdbcCall jdbcCall = new
		 * SimpleJdbcCall(datasource).withFunctionName("get_student_name"); //call for
		 * function SqlParameterSource in = new
		 * MapSqlParameterSource().addValue("book_id", id); String name =
		 * jdbcCall.executeFunction(String.class, in);
		 * 
		 * bookList.forEach(f -> f.setAvailableCopyId(copyid)); bookList.forEach(f ->
		 * f.setMakerentable());
		 */

		tx.commit();
		session.close();

		bookList.forEach(f -> f.setAuthorsString());
		bookList.forEach(f -> f.setBookStatus());
		model.addAttribute("books", bookList);
		model.addAttribute("custid", customerId);

		return "rental_home";

	}

	@RequestMapping(value = "/a/rentals/rent", method = RequestMethod.GET)
	public String rentBookCopy(@RequestParam(value = "id", required = false) String id, Model model,Principal p) {
		int customerId = customerRepository.findByEmailAddress(p.getName()).getId();
		Session session = sessionFactory.openSession();
		Transaction tx = session.beginTransaction();
		// id=String.valueOf(model.getAttribute("id"));

		Rental robj = new Rental();
		robj.setBorrowdate(new Date(System.currentTimeMillis()));
		Date returnDate = DateUtils.addMonths(robj.getBorrowdate(), 1);
		robj.setExpectedreturndate(returnDate);
		robj.setCusid(Long.valueOf(customerId));
		robj.setCopyid(Integer.parseInt(id));

		Rental rOut = rentalRepository.save(robj);
		List<Book> bookList = bookRepository.findByDeleted(0);
		
		for (int i = 0; i < bookList.size(); i++) {

			SimpleJdbcCall jdbcCall = new SimpleJdbcCall(datasource).withFunctionName("checkBookAvailabiltyStatus");
			// call for function
			SqlParameterSource in = new MapSqlParameterSource().addValue("book_id", bookList.get(i).getId());
			System.out.println("calling for" + in.getValue("book_id"));
			int copyid = 0;
			try {

				copyid = jdbcCall.executeFunction(Integer.class, in);

			} catch (Exception ex) {
				System.out.println("Exception while running checkBookAvailabiltyStatus for" + in.getValue("book_id"));
			}
			bookList.get(i).setAvailableCopyId(copyid);
			bookList.get(i).setMakerentable();
		}
		
		tx.commit();
		session.close();
		


		
		
		
	
		 session = sessionFactory.openSession();
		 tx = session.beginTransaction(); // id=String.valueOf(model.getAttribute("id"));

		List<Tuple> tupleResult = rentalRepository.findrentalListForCustomer(customerId);
		List<RentalHistory> rentals = new ArrayList<RentalHistory>();
		for (Tuple t : tupleResult) {
			RentalHistory r = new RentalHistory();
			r.rid = Integer.parseInt(String.valueOf(t.get(0)));
			r.cusid = Integer.parseInt(String.valueOf(t.get(1)));
			r.bookid = Integer.parseInt(String.valueOf(t.get(2)));
			r.bookName = bookRepository.getOne(r.bookid).getBookName();
			r.authorsString = bookRepository.getOne(r.bookid).getAuthorsString();
			r.topicName = bookRepository.getOne(r.bookid).getTopic().getTopicName();
			r.copyid = Integer.parseInt(String.valueOf(t.get(3)));
			r.status = Integer.parseInt(String.valueOf(t.get(4)));
			Rental rental=rentalRepository.getOne(r.rid);
			r.actualreturndate=(rental.getActualreturndate()!=null)?rental.getActualreturndate().toString():"";
			r.borrowdate=rental.getBorrowdate().toString();
			r.expectedreturndate=rental.getExpectedreturndate().toString();
			
			if(r.status==0)
			r.StatusText="Returned";
			
			if(r.status==1)
				r.StatusText="Over Due";
			
			if(r.status==2)
				r.StatusText="Not Over Due";
				
			if (r.status != 0) {
				r.isamountPending = true;
			}
			rentals.add(r);
		}

		tx.commit();
		session.close();

		model.addAttribute("rentals", rentals);

		bookList.forEach(f -> f.setAuthorsString());
		bookList.forEach(f -> f.setBookStatus());
		model.addAttribute("books", bookList);
		model.addAttribute("custid", customerId);
		model.addAttribute("rentals", rentals);

		
		return "rental_history";
	}

	@RequestMapping(value = "/a/rental/cust/history", method = RequestMethod.GET)
	public String rentalHistory(@RequestParam(value = "id", required = false) String id, Model model,Principal p) {
		int customerId = customerRepository.findByEmailAddress(p.getName()).getId();
		Session session = sessionFactory.openSession();
		Transaction tx = session.beginTransaction(); // id=String.valueOf(model.getAttribute("id"));

		List<Tuple> tupleResult = rentalRepository.findrentalListForCustomer(customerId);
		List<RentalHistory> rentals = new ArrayList<RentalHistory>();
		for (Tuple t : tupleResult) {
			RentalHistory r = new RentalHistory();
			r.rid = Integer.parseInt(String.valueOf(t.get(0)));
			r.cusid = Integer.parseInt(String.valueOf(t.get(1)));
			r.bookid = Integer.parseInt(String.valueOf(t.get(2)));
			r.bookName = bookRepository.getOne(r.bookid).getBookName();
			r.authorsString = bookRepository.getOne(r.bookid).getAuthorsString();
			r.topicName = bookRepository.getOne(r.bookid).getTopic().getTopicName();
			r.copyid = Integer.parseInt(String.valueOf(t.get(3)));
			r.status = Integer.parseInt(String.valueOf(t.get(4)));
			Rental rental=rentalRepository.getOne(r.rid);
			r.actualreturndate=(rental.getActualreturndate()!=null)?rental.getActualreturndate().toString():"";
			r.borrowdate=rental.getBorrowdate().toString();
			r.expectedreturndate=rental.getExpectedreturndate().toString();
			
			if(r.status==0)
			r.StatusText="Returned";
			
			if(r.status==1)
				r.StatusText="Over Due";
			
			if(r.status==2)
				r.StatusText="Not Over Due";
				
			if (r.status != 0) {
				r.isamountPending = true;
			}
			rentals.add(r);
		}

		tx.commit();
		session.close();

		model.addAttribute("rentals", rentals);

		return "rental_history";
	}
	
	@RequestMapping(value = "/a/rentals/payment", method = RequestMethod.GET)
	public String rentalPayment(@RequestParam(value = "id", required = false) String id, Model model) {

		Session session = sessionFactory.openSession();
		Transaction tx = session.beginTransaction();
		// id=String.valueOf(model.getAttribute("id"));
		Rental r=rentalRepository.getOne(Integer.parseInt(id));
		r.setActualreturndate(new Date(System.currentTimeMillis()));
		Rental rental = rentalRepository.save(r);
	
		tx.commit();
		session.close();
		
		session = sessionFactory.openSession();
		tx = session.beginTransaction();
		Invoice invoice = invoiceRepository.findByRentalid(Integer.parseInt(id)).get(0);
		tx.commit();
		session.close();
		
		
	
		
		
        Payment payment=new Payment();
        
        payment.setPaymentmethodList(Arrays.asList("cash","credit","debit","paypal"));
        payment.setInvoiceid(invoice.getInvoiceid());
        payment.setPaymentamount(invoice.getInvoiceamount());

		model.addAttribute("rental", rental);
		model.addAttribute("invoice", invoice);
		model.addAttribute("payment", payment);
		
		return "rental_payment";
	}
	
	@RequestMapping(value = "/a/rentals/payment", method = RequestMethod.POST)
	public String MakePayment(@RequestParam(value = "id", required = false) String id, Model model,@ModelAttribute("payment") Payment payment,Principal p) {

		Session session = sessionFactory.openSession();
		Transaction tx = session.beginTransaction();
		
		payment.setPaymentdate(new Date(System.currentTimeMillis()));
		
		paymentRepository.save(payment);
		
		tx.commit();
		session.close();
		
		session = sessionFactory.openSession();
		tx = session.beginTransaction(); // id=String.valueOf(model.getAttribute("id"));
		int customerId = customerRepository.findByEmailAddress(p.getName()).getId();
		List<Tuple> tupleResult = rentalRepository.findrentalListForCustomer(customerId);
		List<RentalHistory> rentals = new ArrayList<RentalHistory>();
		for (Tuple t : tupleResult) {
			RentalHistory r = new RentalHistory();
			r.rid = Integer.parseInt(String.valueOf(t.get(0)));
			r.cusid = Integer.parseInt(String.valueOf(t.get(1)));
			r.bookid = Integer.parseInt(String.valueOf(t.get(2)));
			r.bookName = bookRepository.getOne(r.bookid).getBookName();
			r.authorsString = bookRepository.getOne(r.bookid).getAuthorsString();
			r.topicName = bookRepository.getOne(r.bookid).getTopic().getTopicName();
			r.copyid = Integer.parseInt(String.valueOf(t.get(3)));
			r.status = Integer.parseInt(String.valueOf(t.get(4)));
			Rental rental=rentalRepository.getOne(r.rid);
			r.actualreturndate=(rental.getActualreturndate()!=null)?rental.getActualreturndate().toString():"";
			r.borrowdate=rental.getBorrowdate().toString();
			r.expectedreturndate=rental.getExpectedreturndate().toString();
			if (r.status != 0) {
				r.isamountPending = true;
			}
			rentals.add(r);
		}

		tx.commit();
		session.close();

		model.addAttribute("rentals", rentals);

		
		return "rental_history";
	}

}
