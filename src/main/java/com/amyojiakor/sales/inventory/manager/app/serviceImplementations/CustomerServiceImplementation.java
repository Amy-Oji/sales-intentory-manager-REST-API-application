package com.amyojiakor.sales.inventory.manager.app.serviceImplementations;

import com.amyojiakor.sales.inventory.manager.app.models.dto.CustomerOrderDTO;
import com.amyojiakor.sales.inventory.manager.app.models.dto.CustomerOrderResponse;
import com.amyojiakor.sales.inventory.manager.app.models.entities.Customer;
import com.amyojiakor.sales.inventory.manager.app.models.entities.Order;
import com.amyojiakor.sales.inventory.manager.app.models.entities.OrderDetails;
import com.amyojiakor.sales.inventory.manager.app.repositories.CustomerRepository;
import com.amyojiakor.sales.inventory.manager.app.repositories.OrderDetailsRepository;
import com.amyojiakor.sales.inventory.manager.app.repositories.OrderRepository;
import com.amyojiakor.sales.inventory.manager.app.repositories.ProductRepository;
import com.amyojiakor.sales.inventory.manager.app.services.CustomerService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
@Service
public class CustomerServiceImplementation implements CustomerService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    private final CustomerRepository customerRepository;

    private final OrderDetailsRepository orderDetailsRepository;

    @Autowired
    public CustomerServiceImplementation(OrderRepository orderRepository, ProductRepository productRepository,
                                         CustomerRepository customerRepository, OrderDetailsRepository orderDetailsRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.orderDetailsRepository = orderDetailsRepository;
    }


    @Transactional
    @Override
    public CustomerOrderResponse placeOrder(CustomerOrderDTO customerOrderDTO) throws Exception {
        Order order = new Order();
        List<OrderDetails> orderDetailsList = new ArrayList<>();
        Map<Long, Double> productDetails = new HashMap<>();

        Customer customer = new Customer();
        customer.setName(customerOrderDTO.getCustomerName());
        customer.setPhoneNum(customerOrderDTO.getCustomerPhoneNum());
        customerRepository.save(customer);


        Map<Long, Integer> orderProducts = customerOrderDTO.getProducts();
        double sum = 0;

        for (Map.Entry<Long, Integer> entry : orderProducts.entrySet()) {
            long productId = entry.getKey();
            int quantity = entry.getValue();

            var product = productRepository.findById(productId).orElseThrow(() -> new Exception("Product with ID '"+ productId + " does not exists." +
                    " Please make your selection from our existing products"));
            if(quantity > product.getAvailableQuantity()){
                throw  new Exception("Product ID:" + productId + " with name: '" + product.getName() + "' is currently not available" +
                        " Please make your selection from our available products");
            }

            product.setAvailableQuantity(product.getAvailableQuantity() - quantity);
            var price = product.getPrice() * quantity;
            sum = sum + price;

            productDetails.put(productId, price);

            OrderDetails orderDetails = new OrderDetails();
            orderDetails.setProduct(product);
            orderDetails.setQuantityOrdered(quantity);
            orderDetails.setPricePerUnit(product.getPrice());
            orderDetails.setPriceXquantity(price);
            orderDetails.setOrder(order);
            orderDetailsList.add(orderDetails);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var orderTime = LocalDateTime.now(ZoneId.of("Africa/Lagos"));
        String orderTimeStr = orderTime.format(formatter);

        CustomerOrderResponse customerOrderResponse = new CustomerOrderResponse();
        customerOrderResponse.setCustomerOrderDTO(customerOrderDTO);
        customerOrderResponse.setSum(sum);
        customerOrderResponse.setOrder_date(orderTimeStr);
        customerOrderResponse.setProductDetails(productDetails);

        order.setCustomer(customer);
        order.setOrder_date(orderTime);
        order.setSum(sum);
        order.setOrderDetails(orderDetailsList);
        orderRepository.save(order);

        for (OrderDetails orderDetail : orderDetailsList) {
            productRepository.save(orderDetail.getProduct());
        }

        return customerOrderResponse;
    }
}