package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.exception.AddressBookBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressBookServiceimpl implements AddressBookService {
    @Autowired
    AddressBookMapper addressBookMapper;
    @Override
    public List<AddressBook> list() {
        Long currentId = BaseContext.getCurrentId();
        LambdaQueryWrapper<AddressBook>queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId,currentId);
        return addressBookMapper.selectList(queryWrapper);


    }

    @Override
    public void save(AddressBook addressBook) {
        addressBookMapper.insert(addressBook);

    }

    @Override
    public AddressBook getById(Long id) {
        return addressBookMapper.selectById(id);
    }

    @Override
    public void update(AddressBook addressBook) {
        addressBookMapper.updateById(addressBook);

    }

    @Override
    public void setDefault(AddressBook addressBook) {
        List<AddressBook> isDefaultAddressBook = addressBookMapper.getIsDefaultAddressBook(BaseContext.getCurrentId());
        if (isDefaultAddressBook!=null&&isDefaultAddressBook.size()==1)
        {   addressBook.setIsDefault(StatusConstant.ENABLE);
            AddressBook addressBook1 = isDefaultAddressBook.get(0);
            addressBook1.setIsDefault(StatusConstant.DISABLE);
            addressBookMapper.updateById(addressBook1);}
        else
        {
            addressBook.setIsDefault(StatusConstant.ENABLE);
            addressBookMapper.updateById(addressBook);

        }


    }

    @Override
    public void deleteById(Long id) {
        addressBookMapper.deleteById(id);
    }

    @Override
    public List<AddressBook> getdefault() {
        AddressBook addressBook = new AddressBook();
        addressBook.setIsDefault(1);
        addressBook.setUserId(BaseContext.getCurrentId());
        return addressBookMapper.getIsDefaultAddressBook(BaseContext.getCurrentId());
    }
}
