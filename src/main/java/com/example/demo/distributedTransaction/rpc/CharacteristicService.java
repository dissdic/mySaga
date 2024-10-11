package com.example.demo.distributedTransaction.rpc;

import com.example.demo.distributedTransaction.saga.SagaAction;

public interface CharacteristicService {
    @SagaAction(rollbackMethodFullName = "removeCharacteristic",retry = 1)
    public void addArbitraryCharacteristic(Integer userId);

    public void removeCharacteristic(Integer id);
}
