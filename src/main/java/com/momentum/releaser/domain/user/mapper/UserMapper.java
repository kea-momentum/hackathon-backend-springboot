package com.momentum.releaser.domain.user.mapper;

import com.momentum.releaser.domain.user.dto.AuthResponseDto.ConfirmPasswordCodeResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.momentum.releaser.domain.user.domain.User;
import com.momentum.releaser.domain.user.dto.UserResponseDto.UserProfileImgResponseDTO;

@Mapper
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    /**
     * Entity(User) -> DTO(UserProfileImgResponseDto)
     */
    @Mapping(target = "image", source = "img")
    UserProfileImgResponseDTO toUserProfileImgResponseDto(User user);

    /**
     * Entity(User) -> DTO(ConfirmPasswordCodeResponseDTO)
     *
     * @param user 사용자 객체
     * @return ConfirmPasswordCodeResponseDTO
     */
    ConfirmPasswordCodeResponseDTO toConfirmPasswordCodeResponseDTO(User user);
}
