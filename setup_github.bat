@echo off
echo ==============================================
echo KF Music - Local GitHub Repository Setup Tool
echo ==============================================
echo.
set /p git_name="Enter your GitHub user name: "
set /p git_email="Enter your GitHub email address: "
set /p repo_url="Enter your GitHub repository URL (e.g., https://github.com/username/kfmusic.git): "

git config --local user.name "%git_name%"
git config --local user.email "%git_email%"
git remote remove origin >nul 2>&1
git remote add origin "%repo_url%"

echo.
echo Local Git repository configuration successful:
echo ----------------------------------------------
git config --local -l | findstr "user.name user.email"
git remote -v
echo.
echo You can now run 'git push -u origin main' to push your code.
echo ==============================================
pause
