rm -rf ./main.zip
GOOS=linux GOARCH=amd64 go build -o main main.go
zip main.zip main
rm -rf ./main
