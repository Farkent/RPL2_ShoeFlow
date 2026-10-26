package shoeflow.model;

/** Model tabel users (+ nama role hasil JOIN dengan tabel roles). */
public class User {
    private int id;
    private int roleId;
    private String roleName;   // admin / manajer / kasir
    private String name;
    private String username;
    private String password;

    public User() { }

    public User(int id, int roleId, String roleName, String name, String username, String password) {
        this.id = id; this.roleId = roleId; this.roleName = roleName;
        this.name = name; this.username = username; this.password = password;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { this.roleId = roleId; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
