// Author: Richard Bradford

const path = require('path');

module.exports = {
    resolve: {
        extensions: ['.js', '.ts', '.tsx']
    },
    module: {
        rules:   [
            {
                test:    /\.ts$|\.tsx$/,
                include: path.resolve(__dirname, '../'),
                use:     [
                    {
                        loader: 'ts-loader'
                    }
                ]
            }
            , {
                test:    /\.woff2?$|\.ttf$|\.eot$|\.svg$|\.png$/,
                include: path.resolve(__dirname, '../'),
                use:     [
                    {
                        loader: 'url-loader'
                    }
                ]
            }
            , {
                test:    /\.css$/,
                include: path.resolve(__dirname, '../'),
                use:     [
                    {
                        loader: 'style-loader'
                    },
                    {
                        loader:  'css-loader',
                        options: {
                            // Re-direct Semantic UI CSS theme to find assets from installed semantic-ui-css package
                            alias: {
                                './themes/default': 'semantic-ui-css/themes/default'
                            }
                        }
                    }
                ]
            }
        ]
    }
}

