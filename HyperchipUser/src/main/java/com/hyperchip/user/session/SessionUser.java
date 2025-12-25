package com.hyperchip.user.session;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight session object stored in HTTP session as "currentUser".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionUser implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String email;
    private String profileImage; // nullable
}
