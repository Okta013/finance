package ru.anikeeva.finance.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.anikeeva.finance.dto.user.ReadUserListResponse;
import ru.anikeeva.finance.dto.user.ReadUserResponse;
import ru.anikeeva.finance.dto.user.UpdateUserRequest;
import ru.anikeeva.finance.entities.user.User;

@Mapper(componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    ReadUserResponse toReadUserResponse(User user);

    ReadUserListResponse toReadUserListResponse(User users);

    void updateUserFromUpdateUserRequest(UpdateUserRequest updateUserRequest, @MappingTarget User user);
}