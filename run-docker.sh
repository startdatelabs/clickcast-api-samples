container="cc-api-sampler"
running=$(docker inspect --format='{{ .State.Running }}' $container 2> /dev/null)

if [ "$running" != "true" ]; then

  docker pull startdatelabs/clickcast-api-sampler

  docker rm -f "${container}"

  docker run \
    --dns 8.8.8.8 \
    --name "${container}" \
    --net dev.net \
    -h localhost \
    -it \
    -v $(pwd):/usr/src/app \
    -v cc-api-grape-caches:/root/.groovy/grapes \
    -v cc-api-npm-caches:/root/.npm \
    startdatelabs/clickcast-api-sampler

else

  docker exec -it "$container" bash

fi
