package com.db.library.controller;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.db.library.model.Exhibition;
import com.db.library.repository.ExhibitionRepository;

@Controller
@RequestMapping("/exhibition")
public class ExhibitionController {
	
	@Autowired
	private SessionFactory sessionFactory;
	
	@Autowired
	private ExhibitionRepository exhibitionRepository;
	
	
	@RequestMapping(value="/a/getall",method=RequestMethod.GET)
	public String exhibitionList(Model model) {	
		model.addAttribute("exhibitions", exhibitionRepository.findAll());
		return "exhibitions";
		
	}
	
	
	@RequestMapping(value="/create",method=RequestMethod.POST)
	public String authorsAdd(@RequestParam Integer eventId, @RequestParam Double expenses,  Model model) {
		Session session = sessionFactory.openSession();
		Transaction tx = session.beginTransaction();
		Exhibition e1 = new Exhibition(eventId, expenses);
		exhibitionRepository.save(e1);
		tx.commit();
		session.close();
		model.addAttribute("exhibition", exhibitionRepository.findAll());
		return "redirect:/a/exhibition/getall/";
		
	}
	
	@RequestMapping(value="/exhibition/get",method=RequestMethod.GET)
	public Exhibition getExhibitionList(@RequestParam int eventId) {		
		return exhibitionRepository.findById(eventId).get();
	}
}
