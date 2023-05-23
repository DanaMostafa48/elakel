package com.example.toolsproject.Resources;

import com.example.toolsproject.Entities.Meal;
import com.example.toolsproject.Entities.Order;
import com.example.toolsproject.Entities.Restaurant;
import com.example.toolsproject.Entities.Runner;
import com.example.toolsproject.Status.OrderStatus;
import com.example.toolsproject.Status.RunnerStatus;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Path("/order")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {
    private Long findAvailableRunner() {
        String query = "SELECT r.runnerId FROM Runner r WHERE r.status = :status";
        TypedQuery<Long> runnerQuery = entityManager.createQuery(query, Long.class);
        runnerQuery.setParameter("status", RunnerStatus.AVAILABLE);
        List<Long> runnerIds = runnerQuery.getResultList();
        if (runnerIds.isEmpty()) {
            return null;
        }
        return runnerIds.get(0); // Return the first available runner's ID
    }

    private List<Meal> meals = new ArrayList<>();

    @PersistenceContext(unitName = "inMemory")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    @GET
    @Path("/StartOrder")
    public String start_order() {
        Order order = new Order();

        try {
            userTransaction.begin();
            entityManager.persist(order);
            userTransaction.commit();
            return "DONE  " + "order id = " + order.getOrderId();
        } catch (Exception e) {
            e.printStackTrace();
            return " failed ";
        }
    }


    @POST
    @Path("/addMeals/{mealId}/{restaurantId}/{orderId}")
    public String addMeals(@PathParam("restaurantId") Long restaurantId,
                           @PathParam("mealId") Long mealId, @PathParam("orderId") Long orderId) {
        try {

            Restaurant restaurant = entityManager.find(Restaurant.class, restaurantId);
            Meal meal = entityManager.find(Meal.class, mealId);
            if (!restaurant.getMeals().contains(meal)) {
                return "Restaurant doesn't serve this meal.";
            }
            Order order = entityManager.find(Order.class, orderId);

            order.getMeals().add(meal);
            userTransaction.begin();

            entityManager.merge(order);
            userTransaction.commit();


            return "Meals added successfully";
        } catch (Exception e) {
            e.printStackTrace();
            return "Meals addition failed";
        }
    }


    @POST
    @Path("/makeOrder/{orderId}")
    public String makeOrder(@PathParam("orderId") Long orderId) {
        try {
            Order order = entityManager.find(Order.class, orderId);

            float totalPrice = 0;
            List<String> mealNames = new ArrayList<>();
            for (Meal meal : order.getMeals()) {
                totalPrice += meal.getPrice();
                mealNames.add(meal.getName());
            }

            order.setStatus(OrderStatus.PREPARING);

            Long runnerId = findAvailableRunner();
            order.setRunnerId(runnerId);
            Runner runner = entityManager.find(Runner.class, runnerId);

            order.setTotalPrice(totalPrice + runner.getDeliveryFee());

            userTransaction.begin();

            entityManager.merge(order);

            runner.setStatus(RunnerStatus.BUSY);
            entityManager.merge(runner);

            userTransaction.commit();

            String mealNamesString = String.join(", ", mealNames);

            return "Order made successfully. Order ID: " + order.getOrderId() +
                    ", Runner ID: " + order.getRunnerId() +
                    ", Total Price: " + order.getTotalPrice() +
                    ", Meal Names: " + mealNamesString +
                    ", Order Status: " + order.getStatus();
        } catch (Exception e) {
            e.printStackTrace();
            return "Order failed";
        }
    }

    @PUT
    @Path("/editOrder/{orderId}")
    public String editOrderItems(@PathParam("orderId") Long orderId, List<Long> mealIds) {
        try {
            Order order = entityManager.find(Order.class, orderId);

            if (order.getStatus() == OrderStatus.CANCELLED) {
                return "Cannot edit a canceled order.";
            }

            if (order.getStatus() != OrderStatus.PREPARING) {
                return "Order can only be edited in the preparing state.";
            }

            // Remove existing meals from the order
            order.getMeals().clear();

            // Add new meals to the order
            for (Long mealId : mealIds) {
                Meal meal = entityManager.find(Meal.class, mealId);
                if (meal != null) {
                    order.getMeals().add(meal);
                } else {
                    return "Invalid meal ID: " + mealId;
                }
            }

            userTransaction.begin();
            entityManager.merge(order);
            userTransaction.commit();

            // Calculate the new total price and retrieve the updated meal name
            float newTotalPrice = 0;
            String updatedMealName = "";
            for (Meal meal : order.getMeals()) {
                newTotalPrice += meal.getPrice();
                updatedMealName = meal.getName();
            }

            return "Order items updated successfully. New total price: " + newTotalPrice + ", Updated meal name: " + updatedMealName;
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to update order items.";
        }

    }
    @PUT
    @Path("/cancelOrder/{orderId}")
    public String cancelOrder(@PathParam("orderId") Long orderId) {
        try {
            Order order = entityManager.find(Order.class, orderId);

            if (order.getStatus() == OrderStatus.CANCELLED) {
                return "Order is already canceled.";
            }

            if (order.getStatus() != OrderStatus.PREPARING) {
                return "Order can only be canceled in the preparing state.";
            }

            userTransaction.begin();

            order.setStatus(OrderStatus.CANCELLED);
            entityManager.merge(order);

            userTransaction.commit();

            return "Order with ID " + orderId + " has been canceled.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to cancel the order.";
        }
    }
    @PUT
    @Path("/markCompleted/{orderId}/{runnerId}")
    public String markOrderCompleted(@PathParam("orderId") Long orderId, @PathParam("runnerId") Long runnerId) {
        try {
            Order order = entityManager.find(Order.class, orderId);
            Runner runner = entityManager.find(Runner.class, runnerId);

            if (order.getStatus() == OrderStatus.COMPLETED) {
                return "Order is already marked as completed.";
            }

            userTransaction.begin();

            order.setStatus(OrderStatus.COMPLETED);
            order.setRunnerId(runnerId);
            entityManager.merge(order);

            runner.setStatus(RunnerStatus.AVAILABLE);
            entityManager.merge(runner);

            userTransaction.commit();

            return "Order with ID " + orderId + " is now marked as completed with Runner ID: " + runnerId;
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to mark the order as completed.";
        }
    }








}




