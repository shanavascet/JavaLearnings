package org.acme;

import io.grpc.stub.StreamObserver;
import org.acme.user.UserServiceGrpc;
import org.acme.user.UserRequest;
import org.acme.user.UserResponse;

import javax.inject.Singleton;

@Singleton
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    @Override
    public void saveUser(UserRequest request, StreamObserver<UserResponse> responseObserver) {
        // Save user to DB
        UserEntity user = new UserEntity();
        user.name = request.getName();
        user.email = request.getEmail();
        user.password = request.getPassword(); // Note: hash passwords in production
        user.persist();

        UserResponse response = UserResponse.newBuilder()
                .setSuccess(true)
                .setMessage("User saved successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
