package edu.drexel.TrainDemo.order.controllers;

import edu.drexel.TrainDemo.checkout.models.Billing;
import edu.drexel.TrainDemo.order.models.Order;
import edu.drexel.TrainDemo.order.services.OrderRepository;
import edu.drexel.TrainDemo.order.services.OrderService;
import edu.drexel.TrainDemo.trips.models.Itinerary;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class OrderController {
    private OrderService orderService;

    public OrderController(OrderRepository orderRepository) {
        this.orderService = new OrderService(orderRepository);
    }


    public Order createOrder(List<Itinerary> newItinerary, Billing billing) {
        return this.orderService.createOrder(newItinerary, billing);
    }
}
