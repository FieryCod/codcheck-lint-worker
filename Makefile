GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
AWS_ECR_URL:=""

.PHONY: setup
.DEFAULT_GOAL := dockerize

dockerize:
	@rm -Rf target/
	@lein uberjar
	@mv target/*standalone.jar codcheck-lint-worker.jar
	@docker build -f docker/Dockerfile.production . -t fierycod/codcheck-lint-worker
	@docker tag fierycod/codcheck-lint-worker:latest $(AWS_ECR_URL)
	@docker push $(AWS_ECR_URL)
