package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.Orderstatus;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.BeanCopyutil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.webstocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceimpl implements OrderService {
    @Autowired
    OrderMapper orderMapper;
    @Autowired
    AddressBookMapper addressBookMapper;
    @Autowired
    ShoppingcarMapper shoppingcarMapper;
    @Autowired
    OrderDetailMapper orderDetailMapper;
    @Autowired
    WebSocketServer webSocketServer;
    @Autowired
    usermapper userMapper;
    @Autowired
    WeChatPayUtil weChatPayUtil;




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
        log.error(String.valueOf(orders.getId()));
        List<OrderDetail> orderDetails=new ArrayList<>();

        for (ShoppingCart cart : list) {

            OrderDetail orderDetail = BeanCopyutil.copyBean(cart, OrderDetail.class);
            orderDetail .setOrderId(orders.getId());
            orderDetails.add(orderDetail );
            //todo 修改成批量插入

        }
        orderDetailMapper.insertBatch(orderDetails);
shoppingcarMapper.deleteByUserId(BaseContext.getCurrentId());
        return OrderSubmitVO.builder().id(orders.getId()).orderTime(orders.getOrderTime()).orderNumber(orders.getNumber()).orderAmount(orders.getAmount()).build();



    }

    @Override
    public void paySuccess(String outTradeNo) {
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询当前用户的订单
        Orders ordersDB = orderMapper.getByNumberAndUserId(outTradeNo, userId);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orderstatus.TO_BE_CONFIRMED)
                .payStatus(Orderstatus.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.updateById(orders);

        //通过websocket向客户端浏览器推送消息 type orderId content
        Map map = new HashMap();
        map.put("type",1); // 1表示来单提醒 2表示客户催单
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号：" + outTradeNo);

        String json = JSON.toJSONString(map);
        log.warn("webstocket{}",json);
        webSocketServer.sendToAllClient(json);
    }
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.selectById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
JSONObject jsonObject=new JSONObject();
        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
      paySuccess(ordersPaymentDTO.getOrderNumber());//todo 修改支付功能
        return vo;
    }

    @Override
    public void reminder(Long id) {
        Orders orders = orderMapper.selectById(id);

        if (orders==null)
        {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Map map = new HashMap();
        map.put("type",2); //1表示来单提醒 2表示客户催单
        map.put("orderId",id);
        map.put("content","订单号：" + orders.getNumber());

        //通过websocket向客户端浏览器推送消息
        webSocketServer.sendToAllClient(JSON.toJSONString(map));

    }

    @Override
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<OrderDetail> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getId,id);
        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(queryWrapper);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingcarMapper.insertBatch(shoppingCartList);

    }

    @Override
    public void userCancelById(Long id) {
        Orders ordersDB = orderMapper.selectById(id);

        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orderstatus.TO_BE_CONFIRMED)) {
            //调用微信支付退款接口
//            weChatPayUtil.refund(
//                    ordersDB.getNumber(), //商户订单号
//                    ordersDB.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额

            //支付状态修改为 退款
            orders.setPayStatus(Orderstatus.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orderstatus.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(orders);
    }

    @Override
    public OrderVO details(Long id) {
        Orders orders = orderMapper.selectById(id);
        LambdaQueryWrapper<OrderDetail>queryWrapper=new LambdaQueryWrapper<>();
      queryWrapper.eq(OrderDetail::getOrderId,orders.getId());
        // 查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(queryWrapper);

        // 将该订单及其详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        Orders orders1 = orderMapper.selectById(id);
        AddressBook addressBook = addressBookMapper.selectById(orders1.getAddressBookId());
        String ad=addressBook.getProvinceName()+addressBook.getCityName()+addressBook.getDetail();
        orders.setAddress(ad);
        log.warn("地址,"+ad);
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    @Override
    public PageResult pageQuery4User(int page, int pageSize, Integer status) {
//        PageHelper.startPage(page, pageSize);
//
//        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
//        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
//        ordersPageQueryDTO.setStatus(status);
//
//        // 分页条件查询
//        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
//
//        List<OrderVO> list = new ArrayList();
//
//        // 查询出订单明细，并封装入OrderVO进行响应
//        if (page != null && page.getTotal() > 0) {
//            for (Orders orders : page) {
//                Long orderId = orders.getId();// 订单id
//
//                // 查询订单明细
//                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);
//
//                OrderVO orderVO = new OrderVO();
//                BeanUtils.copyProperties(orders, orderVO);
//                orderVO.setOrderDetailList(orderDetails);
//
//                list.add(orderVO);
//            }
//        }
       // return new PageResult(page.getTotal(), list);
        Long currentId = BaseContext.getCurrentId();
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Orders> page1=new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders>queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(currentId!=null,Orders::getUserId,currentId);
        queryWrapper.eq(status!=null,Orders::getStatus,status);
        Page<Orders> ordersPage = orderMapper.selectPage(page1, queryWrapper);

        if (ordersPage.getTotal()==0)
        {
            return  new PageResult(0, null);
        }
        List<Orders> records = ordersPage.getRecords();
        List<OrderVO> list = new ArrayList<>();
        for (Orders orders :records) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);


            OrderVO orderVO = BeanCopyutil.copyBean(orders, OrderVO.class);

            orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        return  new PageResult(ordersPage.getTotal(), list);

    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
//        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
//
//        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
//
//        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
//        List<OrderVO> orderVOList = getOrderVOList(page);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Orders> page=new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        LambdaQueryWrapper<Orders>queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.hasText(ordersPageQueryDTO.getNumber()),Orders::getNumber,ordersPageQueryDTO.getNumber());
        queryWrapper.like(StringUtils.hasText(ordersPageQueryDTO.getPhone()),Orders::getPhone,ordersPageQueryDTO.getPhone());
        queryWrapper.eq(ordersPageQueryDTO.getStatus()!=null,Orders::getStatus,ordersPageQueryDTO.getStatus());
        queryWrapper.eq(ordersPageQueryDTO.getUserId()!=null,Orders::getUserId,ordersPageQueryDTO.getUserId());
       queryWrapper.between(ordersPageQueryDTO.getEndTime()!=null&&ordersPageQueryDTO.getBeginTime()!=null,Orders::getOrderTime,ordersPageQueryDTO.getEndTime(),ordersPageQueryDTO.getEndTime());
queryWrapper.orderByDesc(Orders::getOrderTime);
        Page<Orders> ordersPage = orderMapper.selectPage(page, queryWrapper);
        List<OrderVO> orderVOS = BeanCopyutil.copyBeanList(ordersPage.getRecords(), OrderVO.class);

        return new PageResult(ordersPage.getTotal(), orderVOS);

    }

    @Override
    public OrderStatisticsVO statistics() {
        Integer toBeConfirmed = orderMapper.countStatus(Orderstatus.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orderstatus.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orderstatus.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orderstatus.CONFIRMED)
                .build();

        orderMapper.updateById(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders ordersDB = orderMapper.selectById(ordersRejectionDTO.getId());

        // 订单只有存在且状态为2（待接单）才可以拒单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orderstatus.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == Orderstatus.PAID) {
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
            log.info("申请退款：{}", "拒单退款");
        }

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orderstatus.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.updateById(orders);

    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
//        LambdaQueryWrapper<Orders>queryWrapper=new LambdaQueryWrapper<>();
//        queryWrapper.eq(Orders::getId,ordersCancelDTO.getId());
        Orders ordersDB = orderMapper.selectById(ordersCancelDTO.getId());

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == 1) {
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
            log.info("申请退款：{}", "退款");
        }

        // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orderstatus.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(orders);
    }

    @Override
    public void delivery(Long id) {
        Orders ordersDB = orderMapper.selectById(id);

        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orderstatus.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orderstatus.DELIVERY_IN_PROGRESS);

        orderMapper.updateById(orders);
    }

    @Override
    public void complete(Long id) {
        Orders ordersDB = orderMapper.selectById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orderstatus.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orderstatus.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.updateById(orders);
    }
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        LambdaQueryWrapper<OrderDetail>queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId,orders.getId());
        // 查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(queryWrapper);

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }
}
