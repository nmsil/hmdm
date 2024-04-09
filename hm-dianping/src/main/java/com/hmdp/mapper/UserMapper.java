package com.hmdp.mapper;

import com.hmdp.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /*
     * 添加用户
     * */
    @Insert("insert into tb_user(phone,nick_name,create_time, update_time) value(#{phone},#{nickName},#{createTime},#{updateTime}) ")
    @Options(useGeneratedKeys = true,keyProperty = "id")
    void saveUser(User user);

    /*
    * 根据phone获取用户信息
    * */
    @Select("select * from tb_user where phone = #{phone}")
    User getUserByPhone(String phone);


}
