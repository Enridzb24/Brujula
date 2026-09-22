package pe.brujula.repository;

import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pe.brujula.model.Models.*;

@Repository
public class FinanceRepository {
    private final JdbcClient db;
    public FinanceRepository(JdbcClient db) { this.db = db; }
    public Optional<User> user(String email) {
        return db.sql("SELECT id,name,email,password_hash,role,enabled FROM users WHERE email=:email")
            .param("email",email).query(User.class).optional();
    }
    public void createUser(Register input,String hash) {
        db.sql("INSERT INTO users(name,email,password_hash) VALUES(:name,:email,:hash)")
            .param("name",input.name().strip()).param("email",input.email().strip().toLowerCase(Locale.ROOT))
            .param("hash",hash).update();
    }
    public List<Category> categories(long uid) {
        return db.sql("SELECT id,name,kind FROM categories WHERE user_id=:uid ORDER BY kind,name")
            .param("uid",uid).query(Category.class).list();
    }
    public void createCategory(long uid,CategoryInput input) {
        db.sql("INSERT INTO categories(user_id,name,kind) VALUES(:uid,:name,:kind)")
            .param("uid",uid).param("name",input.name().strip()).param("kind",input.kind()).update();
    }
    public int deleteCategory(long uid,long id) {
        return db.sql("DELETE FROM categories WHERE id=:id AND user_id=:uid").param("id",id).param("uid",uid).update();
    }
    public List<Movement> movements(long uid) {
        return db.sql("""
            SELECT m.id,m.category_id,c.name AS category,c.kind,m.description,m.amount,
                   m.movement_date AS date,m.payment_method
            FROM movements m JOIN categories c ON c.id=m.category_id AND c.user_id=m.user_id
            WHERE m.user_id=:uid ORDER BY m.movement_date DESC,m.id DESC
            """).param("uid",uid).query(Movement.class).list();
    }
    public int saveMovement(long uid,Long id,MovementInput in) {
        var q = db.sql(id == null ? """
            INSERT INTO movements(user_id,category_id,description,amount,movement_date,payment_method)
            VALUES(:uid,:cat,:description,:amount,:date,:method)
            """ : """
            UPDATE movements SET category_id=:cat,description=:description,amount=:amount,
            movement_date=:date,payment_method=:method WHERE id=:id AND user_id=:uid
            """);
        if(id != null) q.param("id",id);
        return q.param("uid",uid).param("cat",in.categoryId()).param("description",in.description().strip())
            .param("amount",in.amount()).param("date",in.date()).param("method",in.paymentMethod()).update();
    }
    public int deleteMovement(long uid,long id) {
        return db.sql("DELETE FROM movements WHERE id=:id AND user_id=:uid").param("id",id).param("uid",uid).update();
    }
    public List<Budget> budgets(long uid) {
        return db.sql("""
            SELECT b.id,b.category_id,c.name AS category,b.month_key AS `month`,b.amount
            FROM budgets b JOIN categories c ON c.id=b.category_id AND c.user_id=b.user_id
            WHERE b.user_id=:uid ORDER BY b.month_key DESC,c.name
            """).param("uid",uid).query(Budget.class).list();
    }
    public void saveBudget(long uid,BudgetInput in) {
        var existing = db.sql("SELECT id FROM budgets WHERE user_id=:uid AND category_id=:cat AND month_key=:month")
            .param("uid",uid).param("cat",in.categoryId()).param("month",in.month()).query(Long.class).optional();
        if(existing.isPresent()) {
            db.sql("UPDATE budgets SET amount=:amount WHERE id=:id AND user_id=:uid")
                .param("amount",in.amount()).param("id",existing.get()).param("uid",uid).update();
        } else {
            db.sql("INSERT INTO budgets(user_id,category_id,month_key,amount) VALUES(:uid,:cat,:month,:amount)")
                .param("uid",uid).param("cat",in.categoryId()).param("month",in.month()).param("amount",in.amount()).update();
        }
    }
    public int deleteBudget(long uid,long id) {
        return db.sql("DELETE FROM budgets WHERE id=:id AND user_id=:uid").param("id",id).param("uid",uid).update();
    }
    public List<Goal> goals(long uid) {
        return db.sql("SELECT id,name,target,saved,deadline FROM goals WHERE user_id=:uid ORDER BY deadline,id")
            .param("uid",uid).query(Goal.class).list();
    }
    public int saveGoal(long uid,Long id,GoalInput in) {
        var q = db.sql(id==null ? "INSERT INTO goals(user_id,name,target,saved,deadline) VALUES(:uid,:name,:target,:saved,:deadline)"
            : "UPDATE goals SET name=:name,target=:target,saved=:saved,deadline=:deadline WHERE id=:id AND user_id=:uid");
        if(id!=null) q.param("id",id);
        return q.param("uid",uid).param("name",in.name().strip()).param("target",in.target())
            .param("saved",in.saved()).param("deadline",in.deadline()).update();
    }
    public int deleteGoal(long uid,long id) {
        return db.sql("DELETE FROM goals WHERE id=:id AND user_id=:uid").param("id",id).param("uid",uid).update();
    }
    public List<AdminUser> users() {
        return db.sql("SELECT id,name,email,role,enabled FROM users ORDER BY id DESC").query(AdminUser.class).list();
    }
    public int setEnabled(long id,boolean enabled) {
        return db.sql("UPDATE users SET enabled=:enabled WHERE id=:id").param("enabled",enabled).param("id",id).update();
    }
    public int promote(String email) {
        return db.sql("UPDATE users SET role='ADMIN',enabled=TRUE WHERE email=:email").param("email",email).update();
    }
}
