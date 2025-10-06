@echo off
chcp 65001 >nul
REM Relic 编译脚本
REM 自动设置Java环境并编译项目

echo.
echo ========================================
echo   Relic 插件编译脚本
echo ========================================
echo.

REM 设置Java 21环境
REM 修改为你的Java环境路径
set "JAVA_HOME=F:\Program Files\jdk-21"
echo [1/3] 设置JAVA_HOME: %JAVA_HOME%
echo.

REM 验证Maven版本
echo [2/3] 验证Maven环境...
call mvn -version
echo.

REM 执行编译
echo [3/3] 开始编译...
echo.
call mvn clean package

REM 检查编译结果
if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo   编译成功！
    echo ========================================
    echo.
    echo 编译产物位置: target/
    echo.
    
    REM 显示文件信息
    if exist "target\relic-system-1.0.0.jar" (
        for %%F in ("target\relic-system-1.0.0.jar") do (
            set /a size=%%~zF/1024
            echo 文件大小: !size! KB
            echo 文件路径: %%~fF
        )
    )
    echo.
    echo 编译成功！可以将JAR文件部署到服务器了。
) else (
    echo.
    echo ========================================
    echo   编译失败！
    echo ========================================
    echo.
    echo 请检查上方的错误信息
)

echo.
