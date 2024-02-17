package middleware

sealed abstract class Role

object Role {
  case class USER(roles: Set[Permission] = Set.empty[Permission]) extends Role

  case class EMPLOYEE(roles: Set[Permission] = Set.empty[Permission]) extends Role

  case class ADMIN(
      permissions: Set[Permission] = Set(
        Permission.ADMIN_CREATE,
        Permission.ADMIN_READ,
        Permission.ADMIN_UPDATE,
        Permission.ADMIN_DELETE,
        Permission.MANAGER_CREATE,
        Permission.MANAGER_UPDATE,
        Permission.MANAGER_READ,
        Permission.MANAGER_DELETE
      )
  ) extends Role
  case class MANAGER(
      permissions: Set[Permission] =
        Set(Permission.MANAGER_CREATE, Permission.MANAGER_UPDATE, Permission.MANAGER_READ, Permission.MANAGER_DELETE)
  ) extends Role
}
