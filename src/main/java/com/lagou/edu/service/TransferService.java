package com.lagou.edu.service;

/**
 * @author 应癫
 */
public interface TransferService {

    //转账接口
    void transfer(String fromCardNo,String toCardNo,int money) throws Exception;

}
