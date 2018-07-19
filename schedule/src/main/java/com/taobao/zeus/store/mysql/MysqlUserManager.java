package com.taobao.zeus.store.mysql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Expression;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import com.taobao.zeus.store.UserManager;
import com.taobao.zeus.store.mysql.persistence.ZeusUser;
@SuppressWarnings("unchecked")
public class MysqlUserManager extends HibernateDaoSupport implements UserManager{
	
	public List<ZeusUser> getAllUsers(){
		return (List<ZeusUser>) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException,
					SQLException {
				Query query=session.createQuery("from com.taobao.zeus.store.mysql.persistence.ZeusUser");
				return query.list();
			}
		});
	}
	
	public ZeusUser findByUid(String uid){
		DetachedCriteria criteria=DetachedCriteria.forClass(ZeusUser.class);
		criteria.add(Expression.eq("uid", uid));
		List<ZeusUser> users=getHibernateTemplate().findByCriteria(criteria);
		if(users!=null && !users.isEmpty()){
			return users.get(0);
		}
		return null;
	}
	
	public List<ZeusUser> findListByUid(final List<String> uids){
		if(uids.isEmpty()){
			return new ArrayList<ZeusUser>();
		}
		return (List<ZeusUser>) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException,
					SQLException {
				Query query=session.createQuery("from com.taobao.zeus.store.mysql.persistence.ZeusUser where uid in (:idList)");
				query.setParameterList("idList", uids);
				return query.list();
			}
		});
	} 
	
	public ZeusUser addOrUpdateUser(final ZeusUser user){
		List<ZeusUser> list=(List<ZeusUser>) getHibernateTemplate().execute(new HibernateCallback() {
			
			@Override
			public Object doInHibernate(Session session) throws HibernateException,
					SQLException {
				Query query=session.createQuery("from com.taobao.zeus.store.mysql.persistence.ZeusUser where uid=?");
				query.setParameter(0, user.getUid());
				return query.list();
			}
		});
		if(list!=null && !list.isEmpty()){
			ZeusUser zu=list.get(0);
			zu.setEmail(user.getEmail());
			zu.setWangwang(user.getWangwang());
			zu.setName(user.getName());
			if(user.getPhone()!=null && !"".equals(user.getPhone())){
				zu.setPhone(user.getPhone());
			}
			zu.setGmtModified(new Date());
			getHibernateTemplate().update(zu);
		}else{
			user.setGmtCreate(new Date());
			user.setGmtModified(new Date());
			getHibernateTemplate().save(user);
		}
		return user;
	}
}
