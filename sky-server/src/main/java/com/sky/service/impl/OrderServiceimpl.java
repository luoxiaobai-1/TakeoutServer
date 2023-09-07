package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.Orderstatus;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingcarMapper;
import com.sky.service.OrderService;
import com.sky.utils.BeanCopyutil;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceimpl implements OrderService {
    @Autowired
    OrderMapper orderMapper;
    @Autowired
    AddressBookMapper addressBookMapper;
    @Autowired
    ShoppingcarMapper shoppingcarMapper;
    @Autowired
    OrderDetailMapper orderDetailMapper;



    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook addressBook = addressBookMapper.selectById(addressBookId);
        if (addressBook==null)
        {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        ShoppingCart shoppingCart=new ShoppingCart();
        shoppingCart.setUserId(BaseContext.getCurrentId());

        List<ShoppingCart> list = shoppingcarMapper.list(shoppingCart);
        if (list==null||list.size()==0)
        {
            throw  new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        Orders orders = BeanCopyutil.copyBean(ordersSubmitDTO, Orders.class);
         orders.setOrderTime(LocalDateTime.now());
         orders.setStatus(Orderstatus.PENDING_PAYMENT);
         orders.setPayStatus(Orderstatus.UN_PAID);
         orders.setNumber(String.valueOf(System.currentTimeMillis()));
         orders.setPhone(addressBook.getPhone());
         orders.setConsignee(addressBook.getConsignee());
         orders.setUserId(BaseContext.getCurrentId());
        orderMapper.insert(orders);
        for (ShoppingCart cart : list) {
            OrderDetail orderDetail=new OrderDetail();
            OrderDetail orderDetail1 = BeanCopyutil.copyBean(cart, OrderDetail.class);
            orderDetail1.setOrderId(orders.getId());
            orderDetailMapper.insert(orderDetail);
            //todo 修改成批量插入

        }
shoppingcarMapper.deleteById(BaseContext.getCurrentId());
        return OrderSubmitVO.builder().id(orders.getId()).orderTime(orders.getOrderTime()).orderNumber(orders.getNumber()).orderAmount(orders.getAmount()).build();



    }
}
