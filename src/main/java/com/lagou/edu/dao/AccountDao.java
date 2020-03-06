package com.lagou.edu.dao;

import com.lagou.edu.pojo.Account;

/**
 * @author 应癫
 */
public interface AccountDao {

    //查询金额
    Account queryAccountByCardNo(String cardNo) throws Exception;

    //修改金额
    int updateAccountByCardNo(Account account) throws Exception;

}
