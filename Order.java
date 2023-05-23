package com.example.toolsproject.Entities;

import com.example.toolsproject.Status.OrderStatus;

import com.example.toolsproject.Status.RunnerStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;
import java.util.Set;

@Entity
@Table(name = "Orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {




    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;



    private float totalPrice;
    private Long runnerId;

    private Long restaurantId;
    private String Meal_name;


    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private RunnerStatus Rustatus;


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "MealsXOrders",
            joinColumns = @JoinColumn(name = "orderId"),
            inverseJoinColumns = @JoinColumn(name = "mealId"))
    private Set<Meal> meals;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "restaurantId", insertable = false, updatable = false)
    private Restaurant restaurant;

}
