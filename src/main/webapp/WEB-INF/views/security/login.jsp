<%--
  Created by IntelliJ IDEA.
  User: ihojin
  Date: 25. 7. 20.
  Time: 오후 2:41
  To change this template use File | Settings | File Templates.
--%>
<%--<body>--%>
<%--<h1>login</h1>--%>
<%--<form name='f' action='/security/login' method='POST'>--%>
<%--    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>--%>
<%--    <table>--%>
<%--        <tr>--%>
<%--            <td>User:</td>--%>
<%--            <td><input type='text' name='username' value=''></td>--%>
<%--        </tr>--%>
<%--        <tr>--%>
<%--            <td>Password:</td>--%>
<%--            <td><input type='password' name='password'/></td>--%>
<%--        </tr>--%>
<%--        <tr>--%>
<%--            <td colspan='2'>--%>
<%--                <input name="submit" type="submit" value="Login"/>--%>
<%--            </td>--%>
<%--        </tr>--%>
<%--    </table>--%>
<%--</form>--%>
<%--</body>--%>
<%--</html>--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Login Page</title>
</head>
<body>
<h1>Login</h1>

<form name="f" action="/security/login" method="POST">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
    <table>
        <tr>
            <td>User:</td>
            <td><input type="text" name="username" value=""/></td>
        </tr>
        <tr>
            <td>Password:</td>
            <td><input type="password" name="password"/></td>
        </tr>
        <tr>
            <td colspan="2">
                <input name="submit" type="submit" value="Login"/>
            </td>
        </tr>
    </table>
</form>

<c:if test="${not empty param.error}">
    <p style="color:red;">로그인 실패: 아이디 또는 비밀번호를 확인하세요.</p>
</c:if>

</body>
</html>
