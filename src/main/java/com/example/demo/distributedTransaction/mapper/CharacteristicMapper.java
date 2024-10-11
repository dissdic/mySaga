package com.example.demo.distributedTransaction.mapper;

import com.example.demo.distributedTransaction.bean.Characteristic;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CharacteristicMapper {

    public void addCharacteristic(Characteristic character);

    public void removeCharacteristic(Integer id);
}
