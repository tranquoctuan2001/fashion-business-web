package com.project.fashionbusinesswebsite.service;

import com.project.fashionbusinesswebsite.domain.CartEntity;
import com.project.fashionbusinesswebsite.domain.CustomerEntity;
import com.project.fashionbusinesswebsite.model.cart.CartDTO;
import com.project.fashionbusinesswebsite.model.cart.CartRequest;
import com.project.fashionbusinesswebsite.model.cart.CartResponse;
import com.project.fashionbusinesswebsite.model.product.ProductViewResponse;
import com.project.fashionbusinesswebsite.repository.CartRepo;
import com.project.fashionbusinesswebsite.utils.CartConstantUtil;
import com.project.fashionbusinesswebsite.utils.FinderUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;


@Service
public class CartService {
    @Autowired
    private CartRepo cartRepo;
    @Autowired
    private ProductService productService;
    @Autowired
    private ModelMapper mapper;
    @Autowired
    private FinderUtil finderUtil;


    public boolean createCart(CartRequest request, Principal principal) {
        ProductViewResponse product = productService.getProductById(request.getProductsId());
        if (ObjectUtils.isEmpty(product)) {
            throw new ServiceException("Không thể tìm thấy sản phẩm với id = " + request.getProductsId());
        }
        if (ObjectUtils.isEmpty(principal)) {
            throw new ServiceException("Vui lòng đăng nhập để tiếp tục");
        }
        User user = (User) ((Authentication) principal).getPrincipal();
        CustomerEntity customerEntity = finderUtil.findCustomerByUserName(user.getUsername());
        CartEntity cartEntity = mapper.map(request, CartEntity.class);
        cartEntity.setStatus(CartConstantUtil.CART_CREAT);
        cartEntity.setDate(new Date());
        cartEntity.setCustomerId(customerEntity.getCustomerId());

        cartRepo.save(cartEntity);
        return true;
    }

    public boolean updateCartAfterPayment(Principal principal) {
        if (ObjectUtils.isEmpty(principal)) {
            throw new ServiceException("Vui lòng đăng nhập để tiếp tục");
        }
        User user = (User) ((Authentication) principal).getPrincipal();
        CustomerEntity customerEntity = finderUtil.findCustomerByUserName(user.getUsername());
        List<CartEntity> listCartEntities = cartRepo.findAllCartsByCustomerWithStatusActive(customerEntity.getCustomerId(), CartConstantUtil.CART_EXPIRE);
        if (CollectionUtils.isNotEmpty(listCartEntities)) {
            listCartEntities.stream().filter(ObjectUtils::isNotEmpty).forEach(y -> {
                CartEntity cartEntity = this.findCartById(y.getCartId());
                cartEntity.setStatus(CartConstantUtil.CART_EXPIRE);

                cartRepo.save(cartEntity);
            });
        }
        return true;
    }

    public List<CartResponse> getAllCartByCustomerName(Principal principal) {
        if (ObjectUtils.isEmpty(principal)) {
            throw new ServiceException("Vui lòng đăng nhập để tiếp tục");
        }
        User user = (User) ((Authentication) principal).getPrincipal();
        CustomerEntity customerEntity = finderUtil.findCustomerByUserName(user.getUsername());
        List<CartEntity> listCartEntities = cartRepo.findAllCartsByCustomerWithStatusActive(customerEntity.getCustomerId(), CartConstantUtil.CART_EXPIRE);
        List<CartResponse> responses = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(listCartEntities)) {
            listCartEntities.stream().filter(ObjectUtils::isNotEmpty).forEach(y -> {
                CartResponse cartResponse = mapper.map(y, CartResponse.class);
                ProductViewResponse product = productService.getProductById(y.getProductsId());
                cartResponse.setProductTitle(product.getProductTitle());
                cartResponse.setProductImg(product.getProductImg());
                cartResponse.setMaxQuantity(product.getProductQuantity());

                List<Double> listDiscounts = finderUtil.getAllDiscountsByProductId(product.getProductsId());
                cartResponse.setProductPrice(ProductService.calculatePriceAfterDiscount(product.getProductPrice(), listDiscounts));
                cartResponse.setProductsId(product.getProductsId());
                responses.add(cartResponse);
            });
        }
        return responses;
    }

    public CartEntity findCartById(int id) {
        Optional<CartEntity> optionalCartEntity = cartRepo.findById(id);
        if (optionalCartEntity.isPresent()) {
            return optionalCartEntity.get();
        }
        throw new ServiceException("Không thể tìm thấy giỏ hàng với id = " + id);
    }

    public boolean removeCart(int id, Principal principal) {
        if (ObjectUtils.isEmpty(principal)) {
            throw new ServiceException("Vui lòng đăng nhập để tiếp tục");
        }
        CartEntity cartEntity = this.findCartById(id);
        cartRepo.delete(cartEntity);
        return true;
    }

    public boolean saveAllCarts(List<CartDTO> listCartDTOs) {
        if (CollectionUtils.isNotEmpty(listCartDTOs)) {
            for (CartDTO cart : listCartDTOs) {
                CartEntity cartEntity = this.findCartById(cart.getCartId());
                cartEntity.setMoney(cart.getMoney());
                cartEntity.setQuantity(cart.getQuantity());
                cartEntity.setStatus(CartConstantUtil.CART_EDIT);

                cartRepo.save(cartEntity);
            }
        }
        return true;
    }
}
