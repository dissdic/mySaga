package com.example.demo.distributedTransaction.rpc;

import com.example.demo.distributedTransaction.bean.Characteristic;
import com.example.demo.distributedTransaction.mapper.CharacteristicMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@DubboService(version = "1.0.0", interfaceClass = CharacteristicService.class)
public class CharacteristicServiceImpl implements CharacteristicService {

    @Autowired
    private CharacteristicMapper characteristicMapper;
    @Override
    public void addArbitraryCharacteristic(Integer userId) {
        Characteristic character = new Characteristic();
        character.setMentality("normal");
        character.setPersonality("inclusive");
        character.setDeleted(false);
        character.setUserId(userId);
        characteristicMapper.addCharacteristic(character);
    }

    @Override
    public void removeCharacteristic(Integer id) {
        characteristicMapper.removeCharacteristic(id);
    }
}
