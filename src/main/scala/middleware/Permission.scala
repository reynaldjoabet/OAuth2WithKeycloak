package middleware

sealed abstract class Permission

object Permission {
  case object ADMIN_READ extends Permission
  case object ADMIN_CREATE extends Permission
  case object ADMIN_DELETE extends Permission
  case object ADMIN_UPDATE extends Permission

  case object MANAGER_READ extends Permission
  case object MANAGER_CREATE extends Permission
  case object MANAGER_DELETE extends Permission
  case object MANAGER_UPDATE extends Permission

}
