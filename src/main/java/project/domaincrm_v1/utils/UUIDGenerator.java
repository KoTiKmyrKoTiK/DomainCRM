package project.domaincrm_v1.utils;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class UUIDGenerator implements IdentifierGenerator {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object arg) throws HibernateException {
        try {

            final Method m = arg.getClass().getMethod("getId");

            if (!m.getReturnType().equals(UUID.class)) {
                throw new NoSuchMethodException();
            }
   
            final UUID invoke = (UUID)m.invoke(arg);
            
            return invoke == null ? UUID.randomUUID() : invoke;

        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new HibernateException("invalid entity");
        }
    }
}