### 1. 安装 Docker
确保你的系统上已经安装了 Docker。你可以通过以下命令检查 Docker 是否已安装：

```bash
docker --version
```

如果没有安装，可以参考 [Docker 官方文档](https://docs.docker.com/get-docker/) 进行安装。

### 2. 创建项目目录
创建一个新的目录来存放你的 demo 文件和 Dockerfile：

```bash
mkdir my-demo
cd my-demo
```

### 3. 添加 demo 文件
将你的 demo 文件（例如 `app.py`、`index.html` 等）复制到 `my-demo` 目录中。

### 4. 创建 Dockerfile
在 `my-demo` 目录中创建一个名为 `Dockerfile` 的文件，内容根据你的应用类型而定。以下是一个简单的 Python Flask 应用的示例 Dockerfile：

```dockerfile
# 使用官方 Python 镜像作为基础镜像
FROM python:3.9-slim

# 设置工作目录
WORKDIR /app

# 复制当前目录的内容到容器的 /app 目录
COPY . .

# 安装依赖
RUN pip install --no-cache-dir -r requirements.txt

# 暴露端口
EXPOSE 5000

# 运行应用
CMD ["python", "app.py"]
```

如果你的 demo 是其他类型的应用（如 Node.js、Java 等），Dockerfile 的内容会有所不同。

### 5. 创建依赖文件
如果你的应用有依赖（如 Python 的 `requirements.txt`），请确保在项目目录中创建该文件并列出所有依赖。

### 6. 构建 Docker 镜像
在 `my-demo` 目录中运行以下命令来构建 Docker 镜像：

```bash
docker build -t my-demo .
```

### 7. 运行 Docker 容器
构建完成后，可以使用以下命令运行 Docker 容器：

```bash
docker run -d -p 5000:5000 my-demo
```

这里 `-d` 表示后台运行，`-p 5000:5000` 表示将容器的 5000 端口映射到主机的 5000 端口。

### 8. 访问应用
如果你的应用是一个 Web 应用，可以在浏览器中访问 `http://localhost:5000` 来查看你的 demo。

### 9. 停止和删除容器
如果需要停止和删除容器，可以使用以下命令：

```bash
# 停止容器
docker ps  # 查看正在运行的容器
docker stop <container_id>

# 删除容器
docker rm <container_id>
```

### 10. 清理镜像
如果需要清理未使用的镜像，可以使用以下命令：

```bash
docker rmi <image_id>
```

### 总结
以上是将 demo 文件容器化并部署到 Docker 的基本步骤。根据你的具体应用类型和需求，Dockerfile 和运行命令可能会有所不同。希望这些步骤能帮助你顺利完成容器化部署！