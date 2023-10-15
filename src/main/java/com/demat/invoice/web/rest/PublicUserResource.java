package com.demat.invoice.web.rest;

import com.demat.invoice.aws.service.AmazonS3ServiceImpl;
import com.demat.invoice.aws.service.S3ResolverService;
import com.demat.invoice.aws.service.S3Service;
import com.demat.invoice.repository.search.UserSearchRepository;
import com.demat.invoice.service.UserService;
import com.demat.invoice.service.dto.UserDTO;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api")
public class PublicUserResource {

    private final Logger log = LoggerFactory.getLogger(PublicUserResource.class);

    private final UserService userService;
    private final UserSearchRepository userSearchRepository;

    @Autowired(required = false)
    @Qualifier(AmazonS3ServiceImpl.SERVICE_NAME)
    protected transient S3Service amazonS3Service;

    @Autowired(required = true)
    @Qualifier(S3ResolverService.SERVICE_NAME)
    private transient S3ResolverService s3ResolverService;

    public PublicUserResource(UserSearchRepository userSearchRepository, UserService userService) {
        this.userService = userService;
        this.userSearchRepository = userSearchRepository;
    }

    /**
     * {@code GET /users} : get all users with only the public informations - calling this are allowed for anyone.
     *
     * @param request a {@link ServerHttpRequest} request.
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body all users.
     */
    @GetMapping("/users")
    public Mono<ResponseEntity<Flux<UserDTO>>> getAllPublicUsers(
        ServerHttpRequest request,
        @org.springdoc.api.annotations.ParameterObject Pageable pageable
    ) {
        log.debug("REST request to get all public User names");
        this.amazonS3Service = this.s3ResolverService.initS3Service();
        try {
            amazonS3Service.checkIfBucketExist("test");
        } catch (Exception e) {

        }
        // Specify the path to the resource file relative to the classpath
        String resourcePath = "test.xml";

        // Use the class loader to get the resource's URL
        ClassLoader classLoader = PublicUserResource.class.getClassLoader();
        var resourceUrl = classLoader.getResource(resourcePath);

        if (resourceUrl != null) {
            try {
                // Convert the resource URL to a File object (works in non-JAR environments)
                File resourceFile = new File(resourceUrl.toURI());

                if (resourceFile.exists()) {
                    System.out.println("File exists: " + resourceFile.getAbsolutePath());
                    amazonS3Service.uploadArchive("",resourceFile);
                } else {
                    System.out.println("File does not exist.");
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Resource not found: " + resourcePath);
        }
        return userService
            .countManagedUsers()
            .map(total -> new PageImpl<>(new ArrayList<>(), pageable, total))
            .map(page -> PaginationUtil.generatePaginationHttpHeaders(UriComponentsBuilder.fromHttpRequest(request), page))
            .map(headers -> ResponseEntity.ok().headers(headers).body(userService.getAllPublicUsers(pageable)));
    }

    /**
     * Gets a list of all roles.
     * @return a string list of all roles.
     */
    @GetMapping("/authorities")
    public Mono<List<String>> getAuthorities() {
        return userService.getAuthorities().collectList();
    }

    /**
     * {@code SEARCH /_search/users/:query} : search for the User corresponding to the query.
     *
     * @param query the query to search.
     * @return the result of the search.
     */
    @GetMapping("/_search/users/{query}")
    public Mono<List<UserDTO>> search(@PathVariable String query) {
        return userSearchRepository.search(query).map(UserDTO::new).collectList();
    }
}
