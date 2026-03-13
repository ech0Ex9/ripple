package org.ripple.endpoint.detector.grpc

import org.ripple.endpoint.detector.api.model.EntryType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class GrpcDetectorTest {
    
    private val detector = GrpcDetector()
    
    @Test
    fun `should detect gRPC service implementation`() {
        val sourceCode = """
            package com.example.grpc;
            
            import io.grpc.stub.annotations.GrpcService;
            
            @GrpcService
            public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
                @Override
                public void getUser(GetUserRequest request, StreamObserver<User> responseObserver) {
                    User user = userService.findById(request.getId());
                    responseObserver.onNext(user);
                    responseObserver.onCompleted();
                }
                
                @Override
                public void createUser(CreateUserRequest request, StreamObserver<User> responseObserver) {
                    User user = userService.create(request);
                    responseObserver.onNext(user);
                    responseObserver.onCompleted();
                }
            }
        """.trimIndent()
        
        val results = detector.detect(sourceCode, "src/main/java/com/example/grpc/UserServiceImpl.java", detector.defaultConfig)
        
        assertTrue(results.isNotEmpty())
        assertEquals(EntryType.GRPC, results[0].type)
        assertTrue(results.any { it.name == "getUser" })
        assertTrue(results.any { it.name == "createUser" })
    }
    
    @Test
    fun `should extract service name from class name`() {
        val sourceCode = """
            @GrpcService
            public class OrderServiceImpl extends OrderServiceGrpc.OrderServiceImplBase {
                @Override
                public void createOrder(OrderRequest request, StreamObserver<Order> responseObserver) {}
            }
        """.trimIndent()
        
        val results = detector.detect(sourceCode, "grpc/OrderServiceImpl.java", detector.defaultConfig)
        
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].path?.contains("Order") == true)
    }
}