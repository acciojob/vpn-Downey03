package com.driver.services.impl;

import com.driver.Utility.CountryPicker;
import com.driver.Utility.ServiceChecker;
import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception{
        User user;
        try{
            user = userRepository2.findById(userId).get();
        }catch (Exception e){
            throw new Exception("User Not Found");
        }

        if(user.isConnected()) throw new Exception("Already Connected");

        countryName = countryName.toUpperCase();
        CountryName cntyName ;

        try {
            cntyName = CountryName.valueOf(countryName);
        }catch (Exception e){
            throw new Exception("Country Not Found");
        }

        if(user.getCountry().getCountryName().equals(cntyName)) return user;

        List<ServiceProvider> availableProviders = ServiceChecker.isCountryAvailable(user.getServiceProviders(),cntyName);

        if(availableProviders.size() == 0) throw new Exception("Unable to connect");

        ServiceProvider serviceProvider = ServiceChecker.getLowIDProvider(availableProviders);

        user.setConnected(true);
        String markupIp = cntyName.toCode()+serviceProvider.getId()+user.getId();
        user.setMaskedIP(markupIp);
        Connection connection = new Connection();
        connection.setUser(user);
        connection.setServiceProvider(serviceProvider);
        user.getConnections().add(connection);
        serviceProvider.getConnections().add(connection);
        Country country = user.getCountry();
        country.setCountryName(cntyName);
        country.setCode(cntyName.toCode());
        user.setCountry(country);

        userRepository2.save(user);
        return user;

    }
    @Override
    public User disconnect(int userId) throws Exception {
        User user ;
        try {
            user = userRepository2.findById(userId).get();
        }catch (Exception e){
            throw new Exception("User not found");
        }

        if(!user.isConnected()) throw new Exception("Already disconnected");

        user.setMaskedIP(null);
        user.setConnected(false);

        Country country = user.getCountry();
        String countryName = CountryPicker.getCountryFromIP(user.getOriginalIP());
        CountryName cntyName = CountryName.valueOf(countryName);
        country.setCountryName(cntyName);
        country.setCode(cntyName.toCode());
        user.setCountry(country);

        userRepository2.save(user);
        return user;
    }
    @Override
    public User communicate(int senderId, int receiverId) throws Exception {
        User sender ;
        try {
            sender = userRepository2.findById(senderId).get();
        }catch (Exception e){
            throw new Exception("Sender Not Found");
        }

        User receiver ;
        try {
            receiver = userRepository2.findById(receiverId).get();
        }catch (Exception e){
            throw new Exception("Receiver Not Found");
        }

        CountryName senderCountry = sender.getCountry().getCountryName();
        CountryName receiverCountry = receiver.getCountry().getCountryName();

        if( senderCountry == receiverCountry) return sender;

        String countryName = CountryPicker.getCountryFromIP(receiver.getOriginalIP());
        CountryName cntyName = CountryName.valueOf(countryName);

        List<ServiceProvider> availableProviders = ServiceChecker.isCountryAvailable(sender.getServiceProviders(),cntyName);

        if(availableProviders.size() == 0) throw new Exception("Cannot establish communication");

        Country country = sender.getCountry();
        country.setCountryName(cntyName);
        country.setCode(cntyName.toCode());
        sender.setCountry(country);

        ServiceProvider serviceProvider = ServiceChecker.getLowIDProvider(availableProviders);

        Connection connection = new Connection();
        connection.setUser(sender);
        connection.setServiceProvider(serviceProvider);

        sender.getConnections().add(connection);
        serviceProvider.getConnections().add(connection);

        userRepository2.save(sender);

       return sender;
    }
}
