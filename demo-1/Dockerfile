# 使用基础镜像
FROM python:3.9-slim

# 设置工作目录
WORKDIR /app

# 复制当前目录的内容到容器的 /app 目录
COPY . .

# 安装依赖
RUN pip install -r requirements.txt

# 暴露端口（如果需要）
EXPOSE 5000

# 定义容器启动时执行的命令
CMD ["python", "your_demo_file.py"]